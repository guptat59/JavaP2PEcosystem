# Introduction #

### What is this repository for? ###

* This project creates a peer-to-peer network for file downloading. It resembles some features of Bit-torrent, but much simplified.

### How do I get set up? ###

* Requires JDK 7.
* pom.xml is provided for downloading dependencies.
* Go ahead and run Server.bat to run the server and run Peer.bat to run the Client(s)
* Server.bat and Peer.bat have the required configuration to change the parameters.

## Owner ##
* [Tarun Gupta Akirala](https://github.com/guptat59/) 

## How does this work? ##

1. Start the file owner process, giving a listening port (CL parameters are mentioned in the Server.bat). The server splits up the given file in to 100KB chunks and is ready to distribute the chunks in P2P fashion.
2. Start peer processes, one at a time, giving the file owner’s listening port, the peer’s listening port, and its download neighbor’s listening port. All these CL parameters are configured in Peer.bat
3. Each peer connects to the server’s listening port. The latter creates a new thread to download one or several file chunks to the peer, while its main thread goes back to listening for new peers.
4. After receiving chunk(s) from the file owner, the peer stores them as separate file(s) and creates a summary file, listing the IDs of the chunks it has.
5. The peer then proceeds with two new threads, with one thread listening to its upload neighbor to which it will upload file chunks, and the other thread connecting to its download neighbor.
6. The peer requests for the chunk ID list from the download neighbor, compare with its own to find the missing ones, and download those from the neighbor. At the mean time, it sends its own chunk ID list to its upload neighbor, and upon request uploads chunks to the neighbor.
7. After a peer has all file chunks, it combines them for a single file.
8. A peer should output its activity to its console whenever it receives a chunk, sends a chunk, receives a chunk ID list, sends out a chunk ID list, requests for chunks, or receives such a request.

A maximum of 1GB File has been tested with a total of 100 Clients. 
**Entire code is written is Java, just using the native libraries.**
