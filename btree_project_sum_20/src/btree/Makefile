JAR=btreelib.jar

#this is the name of the given project folder
ASSIGNMENT=btree_project_F18
#ASSIGNMENT=btree_project_sum_20

#change the ASSIGN path to the path where you have downloaded on your computer
ASSIGN=/home/a/ax/axs8838/Fall2018/Project1
#ASSIGN=/home/j/jb/jbd9386/cse5331_DBMS/Project1

#change the JDKPATH if you are not using omega.uta.edu
JDKPATH = /opt/jdk1.6.0_20
LIBPATH = $(ASSIGN)/$(ASSIGNMENT)/lib/$(JAR)
CLASSPATH = $(LIBPATH):$(ASSIGN)/$(ASSIGNMENT)/src
BINPATH = $(JDKPATH)/bin
JAVAC = $(JDKPATH)/bin/javac -classpath $(CLASSPATH)
JAVA  = $(JDKPATH)/bin/java  -classpath $(CLASSPATH)

PROGS = together

all: $(PROGS)

together:*.java
	$(JAVAC) *.java

clean:
	\rm -f *.class *~ \#* core
