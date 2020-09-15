/*
 * @(#) bt.java   98/03/24
 * Copyright (c) 1998 UW.  All Rights Reserved.
 *         Author: Xiaohu Li (xioahu@cs.wisc.edu).
 *
 */

package btree;

import java.io.*;

import diskmgr.*;
import bufmgr.*;
import global.*;
import heap.*;
import btree.*;
/**
 * btfile.java This is the main definition of class BTreeFile, which derives
 * from abstract base class IndexFile. It provides an insert/delete interface.
 */
public class BTreeFile extends IndexFile implements GlobalConst {

	private final static int MAGIC0 = 1989;

	private final static String lineSep = System.getProperty("line.separator");

	private static FileOutputStream fos;
	private static DataOutputStream trace;

	/**
	 * It causes a structured trace to be written to a file. This output is used
	 * to drive a visualization tool that shows the inner workings of the b-tree
	 * during its operations.
	 *
	 * @param filename
	 *            input parameter. The trace file name
	 * @exception IOException
	 *                error from the lower layer
	 */
	public static void traceFilename(String filename) throws IOException {

		fos = new FileOutputStream(filename);
		trace = new DataOutputStream(fos);
	}

	/**
	 * Stop tracing. And close trace file.
	 *
	 * @exception IOException
	 *                error from the lower layer
	 */
	public static void destroyTrace() throws IOException {
		if (trace != null)
			trace.close();
		if (fos != null)
			fos.close();
		fos = null;
		trace = null;
	}

	private BTreeHeaderPage headerPage;
	private PageId headerPageId;
	private String dbname;

	/**
	 * Access method to data member.
	 * 
	 * @return Return a BTreeHeaderPage object that is the header page of this
	 *         btree file.
	 */
	public BTreeHeaderPage getHeaderPage() {
		return headerPage;
	}

	private PageId get_file_entry(String filename) throws GetFileEntryException {
		try {
			return SystemDefs.JavabaseDB.get_file_entry(filename);
		} catch (Exception e) {
			e.printStackTrace();
			throw new GetFileEntryException(e, "");
		}
	}

	private Page pinPage(PageId pageno) throws PinPageException {
		try {
			Page page = new Page();
			SystemDefs.JavabaseBM.pinPage(pageno, page, false/* Rdisk */);
			return page;
		} catch (Exception e) {
			e.printStackTrace();
			throw new PinPageException(e, "");
		}
	}

	private void add_file_entry(String fileName, PageId pageno)
			throws AddFileEntryException {
		try {
			SystemDefs.JavabaseDB.add_file_entry(fileName, pageno);
		} catch (Exception e) {
			e.printStackTrace();
			throw new AddFileEntryException(e, "");
		}
	}

	private void unpinPage(PageId pageno) throws UnpinPageException {
		try {
			SystemDefs.JavabaseBM.unpinPage(pageno, false /* = not DIRTY */);
		} catch (Exception e) {
			e.printStackTrace();
			throw new UnpinPageException(e, "");
		}
	}

	private void freePage(PageId pageno) throws FreePageException {
		try {
			SystemDefs.JavabaseBM.freePage(pageno);
		} catch (Exception e) {
			e.printStackTrace();
			throw new FreePageException(e, "");
		}

	}

	private void delete_file_entry(String filename)
			throws DeleteFileEntryException {
		try {
			SystemDefs.JavabaseDB.delete_file_entry(filename);
		} catch (Exception e) {
			e.printStackTrace();
			throw new DeleteFileEntryException(e, "");
		}
	}

	private void unpinPage(PageId pageno, boolean dirty)
			throws UnpinPageException {
		try {
			SystemDefs.JavabaseBM.unpinPage(pageno, dirty);
		} catch (Exception e) {
			e.printStackTrace();
			throw new UnpinPageException(e, "");
		}
	}

	/**
	 * BTreeFile class an index file with given filename should already exist;
	 * this opens it.
	 *
	 * @param filename
	 *            the B+ tree file name. Input parameter.
	 * @exception GetFileEntryException
	 *                can not ger the file from DB
	 * @exception PinPageException
	 *                failed when pin a page
	 * @exception ConstructPageException
	 *                BT page constructor failed
	 */
	public BTreeFile(String filename) throws GetFileEntryException,
			PinPageException, ConstructPageException {

		headerPageId = get_file_entry(filename);

		headerPage = new BTreeHeaderPage(headerPageId);
		dbname = new String(filename);
		/*
		 * 
		 * - headerPageId is the PageId of this BTreeFile's header page; -
		 * headerPage, headerPageId valid and pinned - dbname contains a copy of
		 * the name of the database
		 */
	}

	/**
	 * if index file exists, open it; else create it.
	 *
	 * @param filename
	 *            file name. Input parameter.
	 * @param keytype
	 *            the type of key. Input parameter.
	 * @param keysize
	 *            the maximum size of a key. Input parameter.
	 * @param delete_fashion
	 *            full delete or naive delete. Input parameter. It is either
	 *            DeleteFashion.NAIVE_DELETE or DeleteFashion.FULL_DELETE.
	 * @exception GetFileEntryException
	 *                can not get file
	 * @exception ConstructPageException
	 *                page constructor failed
	 * @exception IOException
	 *                error from lower layer
	 * @exception AddFileEntryException
	 *                can not add file into DB
	 */
	public BTreeFile(String filename, int keytype, int keysize,
			int delete_fashion) throws GetFileEntryException,
			ConstructPageException, IOException, AddFileEntryException {

		headerPageId = get_file_entry(filename);
		if (headerPageId == null) // file not exist
		{
			headerPage = new BTreeHeaderPage();
			headerPageId = headerPage.getPageId();
			add_file_entry(filename, headerPageId);
			headerPage.set_magic0(MAGIC0);
			headerPage.set_rootId(new PageId(INVALID_PAGE));
			headerPage.set_keyType((short) keytype);
			headerPage.set_maxKeySize(keysize);
			headerPage.set_deleteFashion(delete_fashion);
			headerPage.setType(NodeType.BTHEAD);
		} else {
			headerPage = new BTreeHeaderPage(headerPageId);
		}

		dbname = new String(filename);

	}

	/**
	 * Close the B+ tree file. Unpin header page.
	 *
	 * @exception PageUnpinnedException
	 *                error from the lower layer
	 * @exception InvalidFrameNumberException
	 *                error from the lower layer
	 * @exception HashEntryNotFoundException
	 *                error from the lower layer
	 * @exception ReplacerException
	 *                error from the lower layer
	 */
	public void close() throws PageUnpinnedException,
			InvalidFrameNumberException, HashEntryNotFoundException,
			ReplacerException {
		if (headerPage != null) {
			SystemDefs.JavabaseBM.unpinPage(headerPageId, true);
			headerPage = null;
		}
	}

	/**
	 * Destroy entire B+ tree file.
	 *
	 * @exception IOException
	 *                error from the lower layer
	 * @exception IteratorException
	 *                iterator error
	 * @exception UnpinPageException
	 *                error when unpin a page
	 * @exception FreePageException
	 *                error when free a page
	 * @exception DeleteFileEntryException
	 *                failed when delete a file from DM
	 * @exception ConstructPageException
	 *                error in BT page constructor
	 * @exception PinPageException
	 *                failed when pin a page
	 */
	public void destroyFile() throws IOException, IteratorException,
			UnpinPageException, FreePageException, DeleteFileEntryException,
			ConstructPageException, PinPageException {
		if (headerPage != null) {
			PageId pgId = headerPage.get_rootId();
			if (pgId.pid != INVALID_PAGE)
				_destroyFile(pgId);
			unpinPage(headerPageId);
			freePage(headerPageId);
			delete_file_entry(dbname);
			headerPage = null;
		}
	}

	private void _destroyFile(PageId pageno) throws IOException,
			IteratorException, PinPageException, ConstructPageException,
			UnpinPageException, FreePageException {

		BTSortedPage sortedPage;
		Page page = pinPage(pageno);
		sortedPage = new BTSortedPage(page, headerPage.get_keyType());

		if (sortedPage.getType() == NodeType.INDEX) {
			BTIndexPage indexPage = new BTIndexPage(page,
					headerPage.get_keyType());
			RID rid = new RID();
			PageId childId;
			KeyDataEntry entry;
			for (entry = indexPage.getFirst(rid); entry != null; entry = indexPage
					.getNext(rid)) {
				childId = ((IndexData) (entry.data)).getData();
				_destroyFile(childId);
			}
		} else { // BTLeafPage

			unpinPage(pageno);
			freePage(pageno);
		}

	}

	private void updateHeader(PageId newRoot) throws IOException,
			PinPageException, UnpinPageException {

		BTreeHeaderPage header;
		PageId old_data;

		header = new BTreeHeaderPage(pinPage(headerPageId));

		old_data = headerPage.get_rootId();
		header.set_rootId(newRoot);
		
		// clock in dirty bit to bm so our dtor needn't have to worry about it
		unpinPage(headerPageId, true /* = DIRTY */);


		// ASSERTIONS:
		// - headerPage, headerPageId valid, pinned and marked as dirty

	}

	/**
	 * insert record with the given key and rid
	 *
	 * @param key
	 *            the key of the record. Input parameter.
	 * @param rid
	 *            the rid of the record. Input parameter.
	 * @exception KeyTooLongException
	 *                key size exceeds the max keysize.
	 * @exception KeyNotMatchException
	 *                key is not integer key nor string key
	 * @exception IOException
	 *                error from the lower layer
	 * @exception LeafInsertRecException
	 *                insert error in leaf page
	 * @exception IndexInsertRecException
	 *                insert error in index page
	 * @exception ConstructPageException
	 *                error in BT page constructor
	 * @exception UnpinPageException
	 *                error when unpin a page
	 * @exception PinPageException
	 *                error when pin a page
	 * @exception NodeNotMatchException
	 *                node not match index page nor leaf page
	 * @exception ConvertException
	 *                error when convert between revord and byte array
	 * @exception DeleteRecException
	 *                error when delete in index page
	 * @exception IndexSearchException
	 *                error when search
	 * @exception IteratorException
	 *                iterator error
	 * @exception LeafDeleteException
	 *                error when delete in leaf page
	 * @exception InsertException
	 *                error when insert in index page
	 */
	public void insert(KeyClass key, RID rid) throws KeyTooLongException,
			KeyNotMatchException, LeafInsertRecException,
			IndexInsertRecException, ConstructPageException,
			UnpinPageException, PinPageException, NodeNotMatchException,
			ConvertException, DeleteRecException, IndexSearchException,
			IteratorException, LeafDeleteException, InsertException,
			IOException

	{
		// Validation of the key
		    // No need for validation, since BTTest.GetStuff.getChoice() has to be an integer
		    // Meaning the key will always be an integer
		
		// When the tree is empty
		if (headerPage.get_rootId().pid == INVALID_PAGE) {
			// Create a leaf page
			BTLeafPage newRootPage = new BTLeafPage(headerPage.get_keyType());
			PageId newRootPageId = newRootPage.getCurPage();
			
			// Initialize the leaf page
			newRootPage.setNextPage(new PageId(INVALID_PAGE));
			newRootPage.setPrevPage(headerPage.get_rootId());
			newRootPage.insertRecord(key, rid);
			headerPage.setNextPage(newRootPageId);
			
			// Unpin the page
			unpinPage(newRootPageId, true /* = DIRTY */);
			
			// Update the header page
			updateHeader(newRootPageId);
		} 
		// Insert new data entries to the tree by calling function _insert()
		else {
			// Check for a split
			KeyDataEntry newRootEntry = _insert(key, rid, headerPage.getNextPage());
			
			// Split occurs
			if (newRootEntry != null) {
				// Create an index page
				BTIndexPage newRootPage = new BTIndexPage(headerPage.get_keyType());
				PageId newRootPageId = newRootPage.getCurPage();
				
				// Initialize the index page
				newRootPage.insertKey(newRootEntry.key, ((IndexData)newRootEntry.data).getData());
				newRootPage.setPrevPage(headerPage.get_rootId());
				headerPage.setNextPage(newRootPageId);
				
				// Unpin the page
				unpinPage(newRootPageId, true /* = DIRTY */);
				
				// Update the header page
				updateHeader(newRootPageId);
			}
			
		}// End of else
		
	}

	private KeyDataEntry _insert(KeyClass key, RID rid, PageId currentPageId)
			throws PinPageException, IOException, ConstructPageException,
			LeafDeleteException, ConstructPageException, DeleteRecException,
			IndexSearchException, UnpinPageException, LeafInsertRecException,
			ConvertException, IteratorException, IndexInsertRecException,
			KeyNotMatchException, NodeNotMatchException, InsertException

	{
		// Create an empty page
		Page page = new Page();
		page = pinPage(currentPageId);
		// Create a BTSortedPage - currentpage
		BTSortedPage currentpage = new BTSortedPage(page, AttrType.attrInteger);
		// Create a KeyDataEntry upEntry
		KeyDataEntry upEntry = new KeyDataEntry(key, rid);
		
		// Check if the currentpage is of type Index
		if (currentpage.getType() == NodeType.INDEX) {
			// Create an index page
			BTIndexPage currentIndexPage = new BTIndexPage(currentpage.getCurPage(),headerPage.get_keyType());
			PageId currentIndexPageId = currentIndexPage.getCurPage();
			// PageId of the new key
			PageId nextPageId = currentIndexPage.getPageNoByKey(key);
			
			// Unpin the currentIndexPage
			unpinPage(currentIndexPageId, true /* = DIRTY */);
			
			// Recursive call
			upEntry = _insert(key, rid, nextPageId);
			
			if ( upEntry == null) {
				// No split
				return null;
			} 
			
			// Pin it again
			pinPage(currentIndexPageId);
			
			// If space available
			if (currentIndexPage.available_space() >= BT.getKeyDataLength(upEntry.key, NodeType.INDEX)) 
			{
				currentIndexPage.insertKey(upEntry.key, ((IndexData)upEntry.data).getData());
				unpinPage(currentIndexPageId, true /* = DIRTY */);
				
				// Done
				return null;
			}
			// If no available space
			else {
				BTIndexPage newIndexPage = new BTIndexPage(headerPage.get_keyType());
				PageId newIndexPageId = newIndexPage.getCurPage();
				newIndexPage.setNextPage(new PageId(INVALID_PAGE));
				
				
				KeyDataEntry tmpEntry;
				RID delRID = new RID();
				
				// Loop 1: Copy all keys from currentIndexPage to newIndexPage
				for (tmpEntry = currentIndexPage.getFirst(delRID); 
					 tmpEntry!= null;
					 tmpEntry = currentIndexPage.getFirst(delRID)) {
					
					// Insert the record to newIndexPage
					newIndexPage.insertKey(tmpEntry.key, ((IndexData)tmpEntry.data).getData());
					// Delete the record on currentIndexPage
					currentIndexPage.deleteSortedRecord(delRID);
				}
				
				// Loop 2: Insert first half back to currentIndexPage
				for (tmpEntry = newIndexPage.getFirst(delRID);
					 newIndexPage.available_space() < currentIndexPage.available_space();
					 tmpEntry = newIndexPage.getFirst(delRID)) {
					
					// Insert the record to currentIndexPage
					currentIndexPage.insertKey(tmpEntry.key, ((IndexData)tmpEntry.data).getData());
					// Delete the record on newIndexPage
					newIndexPage.deleteSortedRecord(delRID);
				}
				
				tmpEntry = newIndexPage.getFirst(delRID);
				// Insert the key to the correct index node
				if (BT.keyCompare(upEntry.key, tmpEntry.key) > 0) {
					newIndexPage.insertKey(upEntry.key, ((IndexData)upEntry.data).getData());
				} else {
					currentIndexPage.insertKey(upEntry.key, ((IndexData)upEntry.data).getData());
				}
				
				// Unpin the currentIndexPage
				unpinPage(currentIndexPageId, true /* = DIRTY */);
				// Fill up
				upEntry = newIndexPage.getFirst(delRID);
				// Set the left link of newIndexPage
				newIndexPage.setPrevPage(((IndexData)upEntry.data).getData());
				// Delete the first record from newIndexPage
				newIndexPage.deleteSortedRecord(delRID);
				// Unpin the newIndexPage
				unpinPage(newIndexPageId, true /* = DIRTY */);
				// Set the higher index page in the hierarchy to point to the newIndexPage
				((IndexData)upEntry.data).setData(newIndexPageId);
				
				return upEntry;
			}
		}
		// Else check if currentpage is of type Leaf
		else if (currentpage.getType() == NodeType.LEAF) {
			BTLeafPage currentLeafPage = new BTLeafPage(currentpage.getCurPage(),headerPage.get_keyType());
			PageId currentLeafPageId = currentLeafPage.getCurPage();
			
			// If space available
			if (currentLeafPage.available_space() >= BT.getKeyDataLength(upEntry.key, NodeType.LEAF)) 
			{
				currentLeafPage.insertRecord(upEntry.key, ((LeafData)upEntry.data).getData());
				unpinPage(currentLeafPageId, true /* = DIRTY */);
				
				// Done
				return null;
			}
			// If no available space
			else {
				BTLeafPage newLeafPage = new BTLeafPage(headerPage.get_keyType());
				PageId newLeafPageId = newLeafPage.getCurPage();
				newLeafPage.setNextPage(new PageId(INVALID_PAGE));
				newLeafPage.setPrevPage(currentLeafPageId);
				
				KeyDataEntry tmpEntry;
				RID delRID = new RID();
				
				// Loop 1: Copy all keys from currentLeafPage to newLeafPage
				for (tmpEntry = currentLeafPage.getFirst(delRID); 
					 tmpEntry!= null;
					 tmpEntry = currentLeafPage.getFirst(delRID)) {
					
					// Insert the record to newLeafPage
					newLeafPage.insertRecord(tmpEntry.key, ((LeafData)tmpEntry.data).getData());
					// Delete the record on currentLeafPage
					currentLeafPage.deleteSortedRecord(delRID);
					
				}
				
				
				// Loop 2: Insert first half back to currentLeafPage
				for (tmpEntry = newLeafPage.getFirst(delRID);
				     newLeafPage.available_space() < currentLeafPage.available_space();
					 tmpEntry = newLeafPage.getFirst(delRID)) {
					
					// Insert the record to currentLeafPage
					currentLeafPage.insertRecord(tmpEntry.key, ((LeafData)tmpEntry.data).getData());
					// Delete the record on newLeafPage
					newLeafPage.deleteSortedRecord(delRID);
				}
				
				tmpEntry = newLeafPage.getFirst(delRID);
				// Insert the key to the correct leaf node
				if (BT.keyCompare(upEntry.key, tmpEntry.key) < 0) {
					currentLeafPage.insertRecord(upEntry.key, ((LeafData)upEntry.data).getData());
				} else {
					newLeafPage.insertRecord(upEntry.key, ((LeafData)upEntry.data).getData());
				}
				
				// Set the next link of currentLeafPage
				currentLeafPage.setNextPage(newLeafPageId);
				// Unpin the currentLeafPage
				unpinPage(currentLeafPageId, true /* = DIRTY */);
				
				// Fill up
				tmpEntry = newLeafPage.getFirst(delRID);
				upEntry = new KeyDataEntry(tmpEntry.key, newLeafPageId);
				
				// Unpin the newLeafPage
				unpinPage(newLeafPageId, true /* = DIRTY */);
				
				return upEntry;
			}
		}
		// Else throw insertException
		else {
			throw new InsertException(null, "");
		}

	}

	



	/**
	 * delete leaf entry given its <key, rid> pair. `rid' is IN the data entry;
	 * it is not the id of the data entry)
	 *
	 * @param key
	 *            the key in pair <key, rid>. Input Parameter.
	 * @param rid
	 *            the rid in pair <key, rid>. Input Parameter.
	 * @return true if deleted. false if no such record.
	 * @exception DeleteFashionException
	 *                neither full delete nor naive delete
	 * @exception LeafRedistributeException
	 *                redistribution error in leaf pages
	 * @exception RedistributeException
	 *                redistribution error in index pages
	 * @exception InsertRecException
	 *                error when insert in index page
	 * @exception KeyNotMatchException
	 *                key is neither integer key nor string key
	 * @exception UnpinPageException
	 *                error when unpin a page
	 * @exception IndexInsertRecException
	 *                error when insert in index page
	 * @exception FreePageException
	 *                error in BT page constructor
	 * @exception RecordNotFoundException
	 *                error delete a record in a BT page
	 * @exception PinPageException
	 *                error when pin a page
	 * @exception IndexFullDeleteException
	 *                fill delete error
	 * @exception LeafDeleteException
	 *                delete error in leaf page
	 * @exception IteratorException
	 *                iterator error
	 * @exception ConstructPageException
	 *                error in BT page constructor
	 * @exception DeleteRecException
	 *                error when delete in index page
	 * @exception IndexSearchException
	 *                error in search in index pages
	 * @exception IOException
	 *                error from the lower layer
	 *
	 */
	public boolean Delete(KeyClass key, RID rid) throws DeleteFashionException,
			LeafRedistributeException, RedistributeException,
			InsertRecException, KeyNotMatchException, UnpinPageException,
			IndexInsertRecException, FreePageException,
			RecordNotFoundException, PinPageException,
			IndexFullDeleteException, LeafDeleteException, IteratorException,
			ConstructPageException, DeleteRecException, IndexSearchException,
			IOException {
		if (headerPage.get_deleteFashion() == DeleteFashion.NAIVE_DELETE)
			return NaiveDelete(key, rid);
		else
			throw new DeleteFashionException(null, "");
	}

	/*
	 * findRunStart. Status BTreeFile::findRunStart (const void lo_key, RID
	 * *pstartrid)
	 * 
	 * find left-most occurrence of `lo_key', going all the way left if lo_key
	 * is null.
	 * 
	 * Starting record returned in *pstartrid, on page *pppage, which is pinned.
	 * 
	 * Since we allow duplicates, this must "go left" as described in the text
	 * (for the search algorithm).
	 * 
	 * @param lo_key find left-most occurrence of `lo_key', going all the way
	 * left if lo_key is null.
	 * 
	 * @param startrid it will reurn the first rid =< lo_key
	 * 
	 * @return return a BTLeafPage instance which is pinned. null if no key was
	 * found.
	 */

	BTLeafPage findRunStart(KeyClass lo_key, RID startrid) throws IOException,
			IteratorException, KeyNotMatchException, ConstructPageException,
			PinPageException, UnpinPageException {
		BTLeafPage pageLeaf;
		BTIndexPage pageIndex;
		Page page;
		BTSortedPage sortPage;
		PageId pageno;
		PageId curpageno = null; // iterator
		PageId prevpageno;
		PageId nextpageno;
		RID curRid;
		KeyDataEntry curEntry;

		pageno = headerPage.get_rootId();

		if (pageno.pid == INVALID_PAGE) { // no pages in the BTREE
			pageLeaf = null; // should be handled by
			// startrid =INVALID_PAGEID ; // the caller
			return pageLeaf;
		}

		page = pinPage(pageno);
		sortPage = new BTSortedPage(page, headerPage.get_keyType());

		if (trace != null) {
			trace.writeBytes("VISIT node " + pageno + lineSep);
			trace.flush();
		}

		// ASSERTION
		// - pageno and sortPage is the root of the btree
		// - pageno and sortPage valid and pinned

		while (sortPage.getType() == NodeType.INDEX) {
			pageIndex = new BTIndexPage(page, headerPage.get_keyType());
			prevpageno = pageIndex.getPrevPage();
			curEntry = pageIndex.getFirst(startrid);
			while (curEntry != null && lo_key != null
					&& BT.keyCompare(curEntry.key, lo_key) < 0) {

				prevpageno = ((IndexData) curEntry.data).getData();
				curEntry = pageIndex.getNext(startrid);
			}

			unpinPage(pageno);

			pageno = prevpageno;
			page = pinPage(pageno);
			sortPage = new BTSortedPage(page, headerPage.get_keyType());

			if (trace != null) {
				trace.writeBytes("VISIT node " + pageno + lineSep);
				trace.flush();
			}

		}

		pageLeaf = new BTLeafPage(page, headerPage.get_keyType());

		curEntry = pageLeaf.getFirst(startrid);
		while (curEntry == null) {
			// skip empty leaf pages off to left
			nextpageno = pageLeaf.getNextPage();
			unpinPage(pageno);
			if (nextpageno.pid == INVALID_PAGE) {
				// oops, no more records, so set this scan to indicate this.
				return null;
			}

			pageno = nextpageno;
			pageLeaf = new BTLeafPage(pinPage(pageno), headerPage.get_keyType());
			curEntry = pageLeaf.getFirst(startrid);
		}

		// ASSERTIONS:
		// - curkey, curRid: contain the first record on the
		// current leaf page (curkey its key, cur
		// - pageLeaf, pageno valid and pinned

		if (lo_key == null) {
			return pageLeaf;
			// note that pageno/pageLeaf is still pinned;
			// scan will unpin it when done
		}

		while (BT.keyCompare(curEntry.key, lo_key) < 0) {
			curEntry = pageLeaf.getNext(startrid);
			while (curEntry == null) { // have to go right
				nextpageno = pageLeaf.getNextPage();
				unpinPage(pageno);

				if (nextpageno.pid == INVALID_PAGE) {
					return null;
				}

				pageno = nextpageno;
				pageLeaf = new BTLeafPage(pinPage(pageno),
						headerPage.get_keyType());

				curEntry = pageLeaf.getFirst(startrid);
			}
		}

		return pageLeaf;
	}

	/*
	 * Status BTreeFile::NaiveDelete (const void *key, const RID rid)
	 * 
	 * Remove specified data entry (<key, rid>) from an index.
	 * 
	 * We don't do merging or redistribution, but do allow duplicates.
	 * 
	 * Page containing first occurrence of key `key' is found for us by
	 * findRunStart. We then iterate for (just a few) pages, if necesary, to
	 * find the one containing <key,rid>, which we then delete via
	 * BTLeafPage::delUserRid.
	 */

	private boolean NaiveDelete(KeyClass key, RID rid)
			throws LeafDeleteException, KeyNotMatchException, PinPageException,
			ConstructPageException, IOException, UnpinPageException,
			PinPageException, IndexSearchException, IteratorException {
		// Create a leafPage, delRid and a delete entry.
		BTLeafPage leafPage;
		RID delRid = new RID();
		KeyDataEntry delEntry;

		// Get the leaf node where the key exists
		leafPage = findRunStart(key, rid);
		
		// If the key does not exist in any leaf nodes
		if (leafPage == null) {
			return false;
		}
		
		// Set the delEntry at the beginning of the leaf node
		delEntry = leafPage.getCurrent(delRid);
		
		// The leaf exists, meaning the key does exist on the leaf
		// While loop to skip through the whole leaf node to get to the spot of the key
		while (BT.keyCompare(delEntry.key, key) < 0) {
			delEntry = leafPage.getNext(delRid);
		}
		
		// Delete the entry
		leafPage.delEntry(delEntry);
		// Unpin the leaf page
		unpinPage(leafPage.getCurPage(), true);
		return true;
	}
	/**
	 * create a scan with given keys Cases: (1) lo_key = null, hi_key = null
	 * scan the whole index (2) lo_key = null, hi_key!= null range scan from min
	 * to the hi_key (3) lo_key!= null, hi_key = null range scan from the lo_key
	 * to max (4) lo_key!= null, hi_key!= null, lo_key = hi_key exact match (
	 * might not unique) (5) lo_key!= null, hi_key!= null, lo_key < hi_key range
	 * scan from lo_key to hi_key
	 *
	 * @param lo_key
	 *            the key where we begin scanning. Input parameter.
	 * @param hi_key
	 *            the key where we stop scanning. Input parameter.
	 * @exception IOException
	 *                error from the lower layer
	 * @exception KeyNotMatchException
	 *                key is not integer key nor string key
	 * @exception IteratorException
	 *                iterator error
	 * @exception ConstructPageException
	 *                error in BT page constructor
	 * @exception PinPageException
	 *                error when pin a page
	 * @exception UnpinPageException
	 *                error when unpin a page
	 */
	public BTFileScan new_scan(KeyClass lo_key, KeyClass hi_key)
			throws IOException, KeyNotMatchException, IteratorException,
			ConstructPageException, PinPageException, UnpinPageException

	{
		BTFileScan scan = new BTFileScan();
		if (headerPage.get_rootId().pid == INVALID_PAGE) {
			scan.leafPage = null;
			return scan;
		}

		scan.treeFilename = dbname;
		scan.endkey = hi_key;
		scan.didfirst = false;
		scan.deletedcurrent = false;
		scan.curRid = new RID();
		scan.keyType = headerPage.get_keyType();
		scan.maxKeysize = headerPage.get_maxKeySize();
		scan.bfile = this;

		// this sets up scan at the starting position, ready for iteration
		scan.leafPage = findRunStart(lo_key, scan.curRid);
		return scan;
	}

	void trace_children(PageId id) throws IOException, IteratorException,
			ConstructPageException, PinPageException, UnpinPageException {

		if (trace != null) {

			BTSortedPage sortedPage;
			RID metaRid = new RID();
			PageId childPageId;
			KeyClass key;
			KeyDataEntry entry;
			sortedPage = new BTSortedPage(pinPage(id), headerPage.get_keyType());

			// Now print all the child nodes of the page.
			if (sortedPage.getType() == NodeType.INDEX) {
				BTIndexPage indexPage = new BTIndexPage(sortedPage,
						headerPage.get_keyType());
				trace.writeBytes("INDEX CHILDREN " + id + " nodes" + lineSep);
				trace.writeBytes(" " + indexPage.getPrevPage());
				for (entry = indexPage.getFirst(metaRid); entry != null; entry = indexPage
						.getNext(metaRid)) {
					trace.writeBytes("   " + ((IndexData) entry.data).getData());
				}
			} else if (sortedPage.getType() == NodeType.LEAF) {
				BTLeafPage leafPage = new BTLeafPage(sortedPage,
						headerPage.get_keyType());
				trace.writeBytes("LEAF CHILDREN " + id + " nodes" + lineSep);
				for (entry = leafPage.getFirst(metaRid); entry != null; entry = leafPage
						.getNext(metaRid)) {
					trace.writeBytes("   " + entry.key + " " + entry.data);
				}
			}
			unpinPage(id);
			trace.writeBytes(lineSep);
			trace.flush();
		}

	}

}
