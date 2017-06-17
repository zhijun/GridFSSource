package com.m5.app.dao

import java.nio.ByteBuffer

import akka.NotUsed
import akka.stream.scaladsl.Source
import akka.stream.stage.{GraphStage, GraphStageLogic, OutHandler}
import akka.stream.{Attributes, Outlet, SourceShape}
import akka.util.ByteString
import org.mongodb.scala.gridfs.GridFSDownloadStream

import scala.concurrent.{ExecutionContext, Future}


/**
  * Created by zhijun on 2017/6/15 0015.
  * Wrap GridFSDownloadStream to Akka-Stream Source[ByteString, NotUsed]
  *
  * @param GridFSDownload stream provided by Mongo Scala Driver
  * @param chunkSize   Chunk size to read at a time
  */

case class GridfsSource(stream: GridFSDownloadStream, chunkSize: Int)(implicit ec: ExecutionContext) extends GraphStage[SourceShape[Future[ByteBuffer]]] {
  val out: Outlet[Future[ByteBuffer]] = Outlet("File Stream")
  
  override def shape: SourceShape[Future[ByteBuffer]] = SourceShape(out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {
    setHandler(out, new OutHandler {
      override def onPull(): Unit = {
        val buffer = ByteBuffer.allocate(chunkSize)
        val loaded = stream.read(buffer).toFuture().map(_ => buffer)
        push(out, loaded)
      }

      override def onDownstreamFinish(): Unit = {
        super.onDownstreamFinish()
        stream.close()
        complete(out)
      }
    })
  }
}

object GridfsSource {
  def apply(stream: GridFSDownloadStream)(implicit ec: ExecutionContext): Source[ByteString, NotUsed] = {
    Source.fromGraph(GridfsSource(stream, 512 * 1024)).mapAsync(1)(fb => fb.map { buffer => buffer.flip(); ByteString(buffer) }).takeWhile(_.nonEmpty)
  }
}
