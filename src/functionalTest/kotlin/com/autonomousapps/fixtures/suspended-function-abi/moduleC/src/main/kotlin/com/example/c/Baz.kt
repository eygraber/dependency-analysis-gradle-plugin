package com.example.c
import com.example.b.Foo
import kotlinx.coroutines.runBlocking // Or any other way to run a suspend function

class Baz(private val foo: Foo) {
  fun consume() {
    runBlocking {
      val result = foo.bar()
      println(result.value())
    }
  }
}
