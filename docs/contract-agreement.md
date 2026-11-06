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

## 3. 같이 정할 것

- [ ] **알림 `subject`**: 서버가 알림을 만들 때 굵게 보일 앞부분(모임 제목, 상대 닉네임)을 따로 내려줄 수 있는가. 안 되면 앱이 문장을 쪼개야 한다.
- [ ] **알림 탭 이동 경로**: 아래 경로의 인자가 문진곤 화면과 맞는가.
  - 공지 알림 → `Routes.noticeDetail(meetingId, noticeId)` (8g)
  - 메이트 신청 알림 → `Routes.MATE_REQUEST` (11c)
  - 메이트 수락·모임 개설·신청 알림 → 모임 상세, 없으면 `Routes.mateProfile(userId)` (15)
  - 모임 종료 알림 → `Routes.REVIEW_LIST` (12a)
- [ ] **호스트 영역 연결**: 모임 상세에서 `isHost`일 때 부를 composable 이름과 인자(`HostSection.kt`). 지금은 `MeetingDetailScreen.kt`의 임시 자리다.
- [ ] **신청 → 수락·거절이 이어지는 데이터**: 지금 Fake 기준으로 아래 세 가지가 이어지지 않는다. 호스트 쪽을 만들 때 맞춘다.
  - `apply(meetingId, message)`의 `message`를 Fake가 저장하지 않아, `getJoinRequests`의 `JoinRequest.message`가 항상 비어 있다.
  - `accept`는 `confirmedIds`와 `confirmedCount`만 바꾸고 `members`에는 추가하지 않는다. 정원이 차서 `CLOSED`로 바뀌는 것도 흉내 내지 않는다.
  - 서버라면 수락과 동시에 신청자에게 알림(JOIN_ACCEPTED)이 가야 한다. Fake는 알림을 만들지 않는다.
- [ ] **프로필 숫자·후기 재사용**: `ProfileStats`, `ReceivedReview`를 메이트 프로필(15)에서 그대로 쓸 것인가.
- [ ] **종목 이름 함수**: `ui/common/SportLabel.kt`의 `sportLabel()`을 같이 쓴다. 문진곤 화면에서 같은 이름의 함수를 새로 만들지 않는다.
- [ ] **색 토큰**: `AppTokens.kt`의 `AccentSoft` 등은 스크린샷에서 추정한 값이다. 16번 프레임 캡션 값으로 확정해 공용 파일을 한 번에 고친다.

## 4. 문진곤이 추가할 호출 (이미 약속된 것)

`MeetingRepository`에는 개설·수정·취소, 공지·댓글, 평가를 추가하지 않았다. 그 화면을 만드는 사람이 interface에 추가한다. (`data-model.md`) 호스트의 `getJoinRequests`, `accept`, `reject`는 이미 있다.

## 5. 합의 방법

- 이 문서의 3번 항목을 하나씩 정하고 `[x]`와 결정 내용을 적는다.
- 모두 정해지면 `work-split.md`의 "문진곤과 모델·interface 합의 완료"를 체크한다.
- interface를 바꾸는 변경은 화면 작업과 분리한 PR로 올린다. (project-rules)
