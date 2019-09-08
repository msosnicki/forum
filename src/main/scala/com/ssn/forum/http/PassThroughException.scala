package com.ssn.forum.http

/**
  * Requests not handled by the protected endpoint with this error will be attempted in the subsequent routes
  */
private[http] case object PassThroughException extends Exception(s"Internal marker error")
