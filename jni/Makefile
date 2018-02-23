# WormSim makefile
# First build sundials-2.3.0:
# 1. Download from https://computation.llnl.gov/projects/sundials/sundials-software and unpack.
# 2. ./configure CFLAGS=-fPIC CXXFLAGS=-fPIC; make; sudo make install
# Fix include /usr/lib/jvm/... paths below to jni.h and related header files.

all: wormsim_jni

CC=gcc
CXX=$(CC) -O2 -fPIC -I /usr/lib/jvm/java-1.8.0-openjdk-amd64/include -I /usr/lib/jvm/java-1.8.0-openjdk-amd64/include/linux -I sundials-2.3.0/src/ida -I sundials-2.3.0/include -I sundials-2.3.0/src -Wno-deprecated
LIBS=-L /usr/local/lib -l sundials_ida -l sundials_cvode -l sundials_nvecserial -lm -lstdc++
SRCS=$(shell ls *.cpp)
OBJS=$(subst .cpp,.o,$(SRCS))

wormsim_jni: $(OBJS)
	$(CXX) $(OBJS) $(LIBS) -W -shared -o ../lib/wormsim_jni.so

clean:
	rm -rf $(TARGET) $(OBJS)

depend:
	$(CXX) -MM $(SRCS) > .depends.mk

-include .depends.mk
