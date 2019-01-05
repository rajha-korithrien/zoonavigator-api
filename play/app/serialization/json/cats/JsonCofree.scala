/*
 * Copyright (C) 2019  Ľuboš Kozmon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package serialization.json.cats

import cats.free.Cofree
import cats.Eval
import play.api.libs.json._
import play.api.libs.functional.syntax._

import scala.collection.Traversable
import scala.collection.generic.CanBuildFrom
import scala.language.higherKinds

trait JsonCofree extends DefaultReads with DefaultWrites {

  implicit def cofreeEvalSWrites[S[M] <: Traversable[M], A](
      implicit writesA: Writes[A]
  ): Writes[Eval[S[Cofree[S, A]]]] =
    s => traversableWrites[Cofree[S, A]].writes(s.value)

  implicit def cofreeWrites[S[M] <: Traversable[M], A](
      implicit writesA: Writes[A]
  ): Writes[Cofree[S, A]] =
    (
      (JsPath \ "h").write[A] and
      (JsPath \ "t").lazyWrite[Eval[S[Cofree[S, A]]]](cofreeEvalSWrites[S, A])
    )(unlift(Cofree.unapply[S, A]))

  implicit def cofreeEvalSReads[S[_], A](
      implicit readsA: Reads[A],
      cbf: CanBuildFrom[S[_], Cofree[S, A], S[Cofree[S, A]]]
  ): Reads[Eval[S[Cofree[S, A]]]] =
    traversableReads[S, Cofree[S, A]].map(Eval.now)

  implicit def cofreeReads[S[_], A](
      implicit readsA: Reads[A],
      cbf: CanBuildFrom[S[_], Cofree[S, A], S[Cofree[S, A]]]
  ): Reads[Cofree[S, A]] =
    (
      (JsPath \ "h").read[A] and
      (JsPath \ "t").lazyRead[Eval[S[Cofree[S, A]]]](cofreeEvalSReads)
    ).apply((f1, f2) => Cofree(f1, f2))
}