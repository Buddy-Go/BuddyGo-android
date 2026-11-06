# 데이터 모델 계약

두 사람이 같이 쓰는 모델과 Repository interface의 정의다. 화면을 만들기 전에 읽는다.
근거는 `docs/service-rules.md`이고, 코드는 `data/model/`, `data/repository/`, `data/mock/`에 있다.

## 지켜야 할 것

- 화면과 ViewModel은 `data/model/`의 모델과 Repository interface만 쓴다. (project-rules 6번)
- 필드 **추가**는 자유롭다. 이름 변경과 삭제는 팀에 먼저 알린다. (project-rules 8번)
- 정책 숫자(정원, 데드라인, 체크인 반경, 기본 범위 ±10kg 등)는 모델과 코드에 쓰지 않는다. 서버 설정값이다.
- 호스트/확정/대기/미신청 판단은 `Meeting.roleOf(userId)`에서만 한다.
- 시각은 `Long`(epoch 밀리초)이다. `minSdk 24`라 `java.time`을 쓰지 않는다.
- 비밀번호는 모델에 없다.

## 모델

| 파일 | 모델 | 설명 |
| --- | --- | --- |
| `User.kt` | `User`, `ExerciseRecord`, `Sport`, `Gender`, `SkillLevel`, `HikingStyle` | 기록의 `null`은 '미설정'이다. 활동 지역은 쓰임새가 정해지지 않아 뺐다. |
| `Meeting.kt` | `Meeting`, `MeetingPlace`, `MeetingCriteria`, `MeetingStatus`, `MeetingRole`, `GenderLimit`, `BodyPart` | 종목은 `criteria`의 종류로 정해진다(`meeting.sport`). |
| `JoinRequest.kt` | `JoinRequest`, `JoinRequestStatus` | 호스트에게 가는 신청 정보(닉네임, 매너온도, 기록, 한마디). |
| `Notice.kt`, `Comment.kt` | `Notice`, `Comment` | 공지와 댓글. 답글은 없다. |
| `Review.kt` | `Review`, `ReviewVerdict` | 평가. 제출 후 수정·삭제 불가. |
| `Notification.kt` | `Notification`, `NotificationType` | 알림 18종. 이동에 필요한 id만 담는다. |

### `Meeting` 읽는 법

- `status`: 모집중 → 마감 → 진행중 → 종료, 취소(`CANCELED`)는 어느 단계에서든.
- `capacity`는 호스트 포함 정원이다. 마감은 `confirmedCount == capacity`일 때만이다(`isFull`).
- `confirmedIds`는 호스트를 포함한 확정 참가자다. 명단이 공개되지 않는 사람에게는 서버가 비워서 내려준다. 숫자는 `confirmedCount`를 쓴다.
- `pendingIds`는 호스트에게는 전체, 참가자에게는 본인 것만 내려온다.
- `criteria.Health.hostRecord`가 `null`이면 호스트가 기록을 숨긴 것이다.
- 종료 시각은 `endAtMillis`(= 시작 + 진행 시간)로 계산한다.
- `criteriaJudgement`: 기준 일치 판정(맞음/일부만/안 맞음)은 서버가 계산해 담아 준다. `PARTIAL`이면 범위 밖 부위가 `outOfRangeParts`에 담긴다. `null`이면 판정 문구를 보여 주지 않는다. (service-rules 8장)
- `members`: 확정 참가자의 닉네임과 체크인 여부. 명단이 공개되지 않는 사람에게는 서버가 비워서 내려준다. 체크인 수는 `checkedInCount`.
- `hostReviewCount`: 호스트가 받은 후기 수 (모임 상세의 '후기 32').

## Repository interface

| 이름 | 하는 일 |
| --- | --- |
| `MeetingRepository` | 목록·상세 조회, 찜 켜기·끄기, 체크인, 신청·신청 취소·나가기(참가자), 신청 목록·수락·거절(호스트) |
| `NotificationRepository` | 알림 목록, 읽음 처리(한 건·모두) |
| `UserRepository` | 로그인, 내 정보, 사용자 조회, 프로필 숫자·받은 후기, 알림 설정, 로그아웃 |

- 모든 호출은 `suspend`이고 `Result<T>`를 돌려준다.
- 개설·수정·취소, 공지·댓글, 평가, 메이트, 알림 호출은 아직 없다. **그 화면을 만드는 사람이 interface에 추가한다.**
- 구현은 둘이다. `Fake~Repository`(목데이터)로 먼저 개발하고, `Real~Repository`는 서버가 준비되면 채운다. (지금은 `TODO`)

## 목데이터 (`data/mock/`)

- `MockUsers`: `me`(로그인한 나, 기록 있음), `noRecord`(기록 없음), `hostA`, `hostB`.
- `MockMeetings`: 모집중, 신청 대기, 마감(내가 확정), 진행중, 종료, 취소, 내가 호스트(신청 2명), 빈 목록.
- 내 id는 `u1`이다. 화면을 상태별로 확인할 때 이 목록의 모임을 고른다.

## 아직 정하지 않은 것

- 메이트 모델 (메이트 화면을 만들 때 추가)
- 신청 알림과 푸시 연동 방식
- 평가 태그 목록
- 기준 일치 판정 결과 모델
