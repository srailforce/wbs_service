package utils.extractors

object Base64 {
  def unapply(raw: String): Option[String] = {
    try {
      val decodedBytes = java.util.Base64.getDecoder.decode(raw)
      Option(new String(decodedBytes))
    } catch {
      case _: IllegalArgumentException => None
    }
  }
}