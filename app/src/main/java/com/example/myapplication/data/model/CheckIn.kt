package com.example.myapplication.data.model

// 체크인 (service-rules 7장). 판정은 서버가 하고 앱은 결과를 보여 준다.

// 모임 장소에서 이 거리(미터) 안에서만 체크인에 성공한다. 서버가 정하는 값이고 바뀔 수 있어, 안내 문구에만 쓴다.
const val CHECK_IN_RADIUS_METERS = 50

data class CheckInResult(val checkedInAtMillis: Long)

// 범위 밖이면 이 예외로 실패한다. 서버가 잰 현재 거리를 알려 줘서 '아직 조금 멀어요'에 보여 준다.
class CheckInOutOfRangeException(val distanceMeters: Int) : Exception("모임 장소에서 ${distanceMeters}m 떨어져 있어요")
