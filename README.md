# OpenDSB - core

## Description

OpenDSB is a distributed Open-Source Service Bus. 

> In today's world, real-time information is continuously getting generated by applications (business, social, or any other type), and this information needs easy ways to be reliably and quickly routed to multiple types of receivers. Most of the time, applications that are producing information and applications that are consuming this information are well apart and inaccessible to each other. This, at times, leads to redevelopment of information producers or consumers to provide an integration point between them. Therefore, a mechanism is required for seamless integration of information of producers and consumers to avoid any kind of rewriting of an application at either end. (Nishant Garg)

OpenDSB is an open source, distributed publish-subscribe messaging system, 
mainly designed with the following characteristics:

* High throughput: Keeping big data in mind, OpenDSB is designed to work on commodity hardware and to support millions of messages per second.
* Distributed: OpenDSB explicitly supports messages partitioning over OpenDSB servers and distributing consumption over a cluster of consumer machines while maintaining per-partition ordering semantics.
* Multiple client support: OpenDSB system supports easy integration of clients from different platforms such as Java, JavaScript and Golang.
* Real time: Messages produced by the producer threads should be immediately visible to consumer threads; this feature is critical to event-based systems such as [Complex Event Processing (CEP)](https://en.wikipedia.org/wiki/Complex_event_processing) systems.

In the present big data era, the very first challenge is to collect the data 
as it is a huge amount of data and the second challenge is to analyze it.

Message publishing is a mechanism for connecting various applications with 
the help of messages that are routed between them, for example, by a 
message broker such as OpenDSB broker. OpenDSB broker is a solution to 
the real-time problems of any software solution, that is, to deal with 
real-time information and route it to multiple consumers quickly. 
OpenDSB provides seamless integration between information of producers 
and consumers without blocking the producers of the information, 
and without letting producers know who the final consumers are.

## Architeture

TBD

## Using OpenDSB

### Docker Image

We have an image that includes Java **OpenJDK 9**, build 96 
Available in https://jdk9.java.net/download/ with **REPL** 
support via **jshell**

This image allows you to work in the Service Bus as if it were in a playground.
It provides plenty productivity in the typical development workflow.
