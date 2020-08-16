package us.oyanglul

import cats.data.EitherK

package object luci {
  type EffCons[F[_], Eff[_], A] = EitherK[F, Eff, A]
  type Eff2[F1[_], F2[_], A]    = EffCons[F1, F2, A]
  type Eff3[F1[_], F2[_], F3[_], A] =
    EffCons[F1, Eff2[F2, F3, *], A]
  type Eff4[F1[_], F2[_], F3[_], F4[_], A] =
    EffCons[F1, Eff3[F2, F3, F4, *], A]
  type Eff5[F1[_], F2[_], F3[_], F4[_], F5[_], A] =
    EffCons[F1, Eff4[F2, F3, F4, F5, *], A]
  type Eff6[F1[_], F2[_], F3[_], F4[_], F5[_], F6[_], A] =
    EffCons[F1, Eff5[F2, F3, F4, F5, F6, *], A]
  type Eff7[F1[_], F2[_], F3[_], F4[_], F5[_], F6[_], F7[_], A] =
    EffCons[F1, Eff6[F2, F3, F4, F5, F6, F7, *], A]
  type Eff8[F1[_], F2[_], F3[_], F4[_], F5[_], F6[_], F7[_], F8[_], A] =
    EffCons[F1, Eff7[F2, F3, F4, F5, F6, F7, F8, *], A]
  type Eff9[F1[_], F2[_], F3[_], F4[_], F5[_], F6[_], F7[_], F8[_], F9[_], A] =
    EffCons[F1, Eff8[F2, F3, F4, F5, F6, F7, F8, F9, *], A]
  type Eff10[F1[_], F2[_], F3[_], F4[_], F5[_], F6[_], F7[_], F8[_], F9[_], F10[_], A] =
    EffCons[F1, Eff9[F2, F3, F4, F5, F6, F7, F8, F9, F10, *], A]
  type Eff11[F1[_], F2[_], F3[_], F4[_], F5[_], F6[_], F7[_], F8[_], F9[_], F10[_], F11[_], A] =
    EffCons[F1, Eff10[F2, F3, F4, F5, F6, F7, F8, F9, F10, F11, *], A]
  type Eff12[F1[_], F2[_], F3[_], F4[_], F5[_], F6[_], F7[_], F8[_], F9[_], F10[_], F11[_], F12[_], A] =
    EffCons[F1, Eff11[F2, F3, F4, F5, F6, F7, F8, F9, F10, F11, F12, *], A]
  type Eff13[F1[_], F2[_], F3[_], F4[_], F5[_], F6[_], F7[_], F8[_], F9[_], F10[_], F11[_], F12[_], F13[_], A] =
    EffCons[F1, Eff12[F2, F3, F4, F5, F6, F7, F8, F9, F10, F11, F12, F13, *], A]
  // Are you crazy...
  type Eff14[F1[_], F2[_], F3[_], F4[_], F5[_], F6[_], F7[_], F8[_], F9[_], F10[_], F11[_], F12[_], F13[_], F14[_], A] =
    EffCons[F1, Eff13[F2, F3, F4, F5, F6, F7, F8, F9, F10, F11, F12, F13, F14, *], A]
  type Eff15[F1[_], F2[_], F3[_], F4[_], F5[_], F6[_], F7[_], F8[_], F9[_], F10[_], F11[_], F12[_], F13[_], F14[_], F15[
      _
  ], A] =
    EffCons[F1, Eff14[F2, F3, F4, F5, F6, F7, F8, F9, F10, F11, F12, F13, F14, F15, *], A]
  type Eff16[F1[_], F2[_], F3[_], F4[_], F5[_], F6[_], F7[_], F8[_], F9[_], F10[_], F11[_], F12[_], F13[_], F14[_], F15[
      _
  ], F16[_], A] =
    EffCons[F1, Eff15[F2, F3, F4, F5, F6, F7, F8, F9, F10, F11, F12, F13, F14, F15, F16, *], A]
  type Eff17[F1[_], F2[_], F3[_], F4[_], F5[_], F6[_], F7[_], F8[_], F9[_], F10[_], F11[_], F12[_], F13[_], F14[_], F15[
      _
  ], F16[_], F17[_], A] =
    EffCons[F1, Eff16[F2, F3, F4, F5, F6, F7, F8, F9, F10, F11, F12, F13, F14, F15, F16, F17, *], A]
}
