## Computer Communications and Networking coursework (Spring 2013)

The multiple java files for this repository are the solutions to the assignment problems given for the Spring 2013 Computer Communications and Networking course at the University of Edinburgh.  Each sender-receiver pair of programs are implementations of sending and receiving a given file on localhost.  They vary in features, with sender1-receiver1 implementing basic UDP communication to sender4-receiver4 accumulating more complex features that are used by TCP.  More specifically:

* sender1-receiver1 implements basic UDP
* sender2-receiver2 builds on UDP by adding acknowledgement messages
* sender3-receiver3 adds a sending *window* to the above
* sender4-receiver4 improves on the sending window by selective repeating messages

The helper class provides additional methods used by various of the sender-receiver classes.

### Usage

Compile files with java.  To run the sender and receiver programs use the following in the command line:

* java SenderX localhost <Port> <File> [RetryTimeout] [WindowSize]
* java ReceiverY <Port> <Filename> [WindowSize]