package com.ssn.forum.http

import com.ssn.forum.PostId
import org.http4s.AuthedRoutes

package object auth {
  val SetAuthHeader = "Set-Auth-Token"
  val AuthHeader    = "Auth-Token"

  type AuthPostEndpoint[F[_]] = AuthedRoutes[PostId, F]
}
