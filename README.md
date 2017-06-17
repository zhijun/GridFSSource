# GridFSSource
An Akka-Stream Source wrapper for GridFSDownloadStream in Mongodb scala driver

MongoDB Scala driver provides its unique async in/out streams which cannot be directly integrated into Akka Stream in terms of ByteString.
This wrapper makes it possible.

Usage: 

val stream = mongo.gridfs.openDownloadStream(ObjectId(".."))  
val source: Source[ByteString, NotUsed] = GridfsSource(stream)  
source.runWith(Sink.foreach(println))  

