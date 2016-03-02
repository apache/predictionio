package io.prediction.configuration

/**
  * Created by ykhodorkovsky on 2/26/16.
  */

import java.io.FileInputStream
import java.io.File
import java.security.KeyStore
import javax.net.ssl.{KeyManagerFactory, SSLContext, TrustManagerFactory}

import com.typesafe.config.ConfigFactory
import spray.io.ServerSSLEngineProvider

trait SSLConfiguration {

  private val serverConfig = ConfigFactory.load("server.conf")

  private val keyStoreResource = serverConfig.getString("io.prediction.server.ssl-keystore-resource")
  private val password = serverConfig.getString("io.prediction.server.ssl-keystore-pass")
  private val keyAlias = serverConfig.getString("io.prediction.server.ssl-key-alias")

  private val keyStore = {

    //Loading keystore from specified file
    val clientStore = KeyStore.getInstance("JKS")
    val inputStream = new FileInputStream(getClass().getClassLoader().getResource(keyStoreResource).getFile())
    clientStore.load(inputStream, password.toCharArray)
    inputStream.close()
    clientStore
  }

  //Creating SSL context
  implicit def sslContext: SSLContext = {
    val context = SSLContext.getInstance("TLS")
    val tmf: TrustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm)
    val kmf: KeyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm)
    kmf.init(keyStore, password.toCharArray)
    tmf.init(keyStore)
    context.init(kmf.getKeyManagers, tmf.getTrustManagers, null)
    context
  }

  //provide implicit SSLEngine with some protocols
  implicit def sslEngineProvider: ServerSSLEngineProvider = {
    ServerSSLEngineProvider { engine =>
      engine.setEnabledCipherSuites(Array("TLS_RSA_WITH_AES_256_CBC_SHA", "TLS_ECDH_ECDSA_WITH_RC4_128_SHA", "TLS_RSA_WITH_AES_128_CBC_SHA"))
      engine.setEnabledProtocols(Array("TLSv1", "TLSv1.2", "TLSv1.1"))
      engine
    }
  }
}