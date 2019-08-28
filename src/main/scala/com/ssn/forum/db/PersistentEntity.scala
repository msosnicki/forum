package com.ssn.forum.db

case class PersistentEntity[Id, A](id: Id, entity: A)
