// ----- ostrich service

import com.twitter.finagle.builder.{Server, ServerBuilder}
import com.twitter.finagle.stats.{StatsReceiver, OstrichStatsReceiver}
import com.twitter.finagle.thrift.ThriftServerFramedCodec
import com.twitter.finagle.tracing.{NullTracer, Tracer}
import com.twitter.logging.Logger
import com.twitter.ostrich.admin.Service

trait ThriftServer extends Service with FutureIface {
  val log = Logger.get(getClass)

  def thriftCodec = ThriftServerFramedCodec()
  def statsReceiver: StatsReceiver = new OstrichStatsReceiver
  def tracerFactory: Tracer.Factory = NullTracer.factory
  val thriftProtocolFactory: TProtocolFactory = new TBinaryProtocol.Factory()
  val thriftPort: Int
  val serverName: String

  var server: Server = null

  def start() {
    val thriftImpl = new FinagledService(this, thriftProtocolFactory)
    server = serverBuilder.build(thriftImpl)
  }

  /**
   * You can override this to provide additional configuration
   * to the ServerBuilder.
   */
  def serverBuilder =
    ServerBuilder()
      .codec(thriftCodec)
      .name(serverName)
      .reportTo(statsReceiver)
      .bindTo(new InetSocketAddress(thriftPort))
      .tracerFactory(tracerFactory)

  def shutdown() {
    synchronized {
      if (server != null) {
        server.close(0.seconds)
      }
    }
  }
}
