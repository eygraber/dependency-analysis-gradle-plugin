package com.example.b
import com.example.a.MyResult
interface Foo {
  suspend fun bar(): MyResult
}
