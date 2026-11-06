# 모델·interface 합의 목록 (문찬주 ↔ 문진곤)

`work-split.md`의 "문진곤과 모델·interface 합의 완료"를 닫기 위한 문서다. 문찬주가 `main`에 추가한 공용 계약과, 문진곤과 같이 정해야 할 점을 모았다. 합의한 항목은 `[x]`로 바꾸고 결정을 옆에 적는다.

기준: `main`의 `data/repository/` interface 3개와 `data/model/`.

## 1. 문찬주가 추가한 호출

### MeetingRepository

| 메서드 | 쓰는 화면 | 약속 |
| --- | --- | --- |
| `setFavorite(meetingId, favorite)` | 탐색 카드, 모임 상세 | 켜고 끄는 값을 그대로 보낸다. 같은 값을 다시 보내도 결과가 같다. |
| `leave(meetingId)` | 모임 상세 | 서버가 상태를 정한다. 모집중은 페널티 없이 나가고, 마감이고 데드라인 전이면 모집중으로 돌아가고, 데드라인(시작 90분 전) 후면 모임이 취소된다. (service-rules 5장) |
| `checkIn(meetingId, latitude, longitude)` | 모임 상세 | 서버가 거리를 재서 범위(지금 50m) 안일 때만 성공시킨다. 범위 밖이면 `CheckInOutOfRangeException(distanceMeters)`로 실패한다. 체크인은 한 번만 할 수 있다. (7장) |

### NotificationRepository (새로 추가)

| 메서드 | 약속 |
| --- | --- |
| `getNotifications()` | 최신순 목록. |
| `markAsRead(notificationId)` | 이미 읽은 알림에 다시 보내도 결과가 같다. |
| `markAllAsRead()` | '모두 읽음'. 이미 모두 읽었어도 결과가 같다. |

### UserRepository

| 메서드 | 쓰는 화면 | 약속 |
| --- | --- | --- |
| `getProfileStats(userId)` | 마이페이지(14) | 받은 후기 수, 개설한 모임 수, 많이 받은 태그(많은 순). 메이트 프로필(15)에서도 같이 쓸 수 있다. |
| `getReceivedReviews(userId)` | 마이페이지(14) | 최신순. 한마디와 태그는 공개된다. (11장) |
| `isNotificationEnabled()` / `setNotificationEnabled(enabled)` | 마이페이지(14) | 끄면 전체 알림을 받지 않는다. (12장) |
| `logout()` | 마이페이지(14) | 성공하면 앱이 로그인 화면으로 보낸다. |

## 2. 모델 변경 (공용)

- `Meeting`: `hostReviewCount`, `criteriaJudgement`(기준 일치 판정), `members`(닉네임과 체크인 여부·시각), `deadlineAtMillis`(시작 90분 전으로 계산).
- `Notification`: `subject`(굵게 보이는 앞부분)를 추가했다. 화면은 `subject + message`로 그린다. `message`가 "님"으로 시작하면 붙여 쓰고(서연님이 …) 아니면 한 칸 띄운다.
- 새 모델: `CheckInResult`, `ProfileStats`, `TagCount`, `ReceivedReview`, `GeoPoint`.

## 3. 정한 것

2026-11-06 사용자(문찬주) 판단으로 아래처럼 확정했다. 서버 연결 때 이 약속을 기준으로 맞춘다.

- [x] **알림 `subject`**: 서버가 알림마다 굵게 보일 앞부분(`subject`, 모임 제목이나 상대 닉네임)과 뒤 문장(`message`)을 따로 내려준다. 앱은 이어 붙이기만 한다.
- [x] **알림 탭 이동 경로**: 아래 그대로 확정. 에뮬레이터에서 8g, 11c, 7, 모임 상세 이동을 확인했다.
  - 공지 알림 → `Routes.noticeDetail(meetingId, noticeId)` (8g). 공지가 없으면 오류 화면(16g)이 나온다.
  - 메이트 신청 알림 → `Routes.MATE_REQUEST` (11c)
  - 메이트 수락·모임 개설·신청 알림 → 모임 상세, 없으면 `Routes.mateProfile(userId)` (15)
  - 모임 종료 알림 → `Routes.REVIEW_LIST` (12a)
- [x] **호스트 영역 연결**: 끝났다. 모임 상세에서 호스트면 `HostSection.kt` 부품으로 화면을 구성한다. (PR #62)
- [x] **신청 → 수락·거절이 이어지는 데이터**: Fake에서 이어지도록 고쳤다. 서버도 같은 동작이어야 한다.
  - `apply(meetingId, message)`의 한마디(앞뒤 공백 제거, 비면 없음)와 신청 시각이 `getJoinRequests`의 `JoinRequest`에 그대로 담긴다.
  - 모집중이 아니거나 이미 신청·확정한 모임에 신청하면 실패한다.
  - `accept`는 확정에 넣고, 명단이 공개된 모임이면 `members`에도 넣는다. 정원이 차면 `CLOSED`로 바뀌고 남은 신청은 거절된다.
  - 신청 취소·거절하면 한마디도 지워진다.
  - 수락·거절 알림(JOIN_ACCEPTED, JOIN_REJECTED)은 신청자에게 가야 하는데, Fake는 로그인한 한 사람만 흉내 내서 만들지 않는다. 서버 몫이다.
- [x] **프로필 숫자·후기 재사용**: 마이페이지(14)는 `ProfileStats`/`ReceivedReview`를, 메이트 프로필(15)은 이미 만든 `MateProfile`을 그대로 쓴다. 두 화면이 같은 `TagCount`(문구 `label`, 받은 수 `count`)를 공유한다. 서버 응답이 정해지면 한 모델로 합친다.
- [x] **종목 이름 함수**: `ui/common/SportLabel.kt`의 `sportLabel()` 하나만 쓴다. 같은 이름의 함수를 새로 만들지 않는다.
- [x] **색 토큰**: 현재 `AppTokens.kt` 값으로 확정한다. `AccentSoft`는 `F1FBE0`(흐름 A 값)이다. 16번 프레임 캡션 값과 달라지면 `AppTokens.kt` 한 곳만 고친다.

## 4. 호스트 호출 (추가 완료)

`MeetingRepository`에 개설(`createMeeting`), 수정(`updateMeeting`), 취소(`cancelMeeting`), 삭제(`deleteMeeting`), 신청 목록·수락·거절이 있다. 공지·댓글, 평가, 메이트는 각 화면의 저장소에 있다.

- `updateMeeting(meetingId, meeting)`: 확정 참가자(호스트 제외)가 있으면 서버는 제목·설명·상세주소만 반영한다. 진행중이 된 뒤에는 실패한다. 잠기지 않았을 때 시작을 바꾸면 지금부터 1시간 30분 이후여야 하고, 정원은 확정 인원보다 작을 수 없다. (service-rules 4장)

## 5. 이 문서를 쓰는 방법

- 새 호출이나 모델 변경은 화면 작업과 분리한 PR로 올리고, 이 문서에 약속을 적는다. (project-rules)
