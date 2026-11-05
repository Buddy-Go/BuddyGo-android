# 버디 고 작업 관련 문서

## 1. 참고 문서

- 구조·규칙: `docs/project-rules.md` (파일 위치의 기준)
- 서비스 정책: `docs/service-rules.md`
- 데이터 모델·Repository 계약: `docs/data-model.md`
- 디자인 v4 화면: `docs/design/screens/` (목록은 `INDEX.md`)

## 2. 담당 요약

| 구분 | 담당 | 범위 | 프레임 수 | 비중 |
| --- | --- | --- | --- | --- |
| 흐름 B | 문찬주 | 가입 → 탐색 → 참여 | 45 | 약 41% |
| 공통 | 문찬주 | 로딩·에러 부품, 네비게이션, 공용 모델 | 12 | 약 10% |
| 흐름 A | 문진곤 | 모임 운영 → 사후 관계 | 54 | 약 49% |
- 문찬주 합계 약 51%, 문진곤 합계 약 49%.

## 3. 흐름 B: 가입 → 탐색 → 참여 (문찬주)

사용자가 앱에 들어와서 모임에 참석할 때까지의 흐름이다.

| 순서 | 기능 | 프레임 | 폴더 |
| --- | --- | --- | --- |
| 1 | 로그인 | 1 | `ui/auth/` |
| 2 | 회원가입·프로필·종목 기록 | 2a~2e, 2c-2~2c-5 | `ui/record/` |
| 3 | 탐색 지도·리스트·핀 | 3-After, 3-2~3-5 | `ui/explore/` |
| 4 | 조건 설정(필터)·위치 검색 | 4a~4g-2 | `ui/explore/filter/` |
| 5 | 모임 상세(참가자 시점)·기준 일치 판정 | 5a, 5b, 5c, 5d, 5e~5i, 5l | `ui/meeting/detail/` |
| 6 | 참가 신청·신청 대기·나가기 | 6a, 6b | `ui/meeting/detail/` |
| 7 | 체크인·진행 중 | 9a~9c | `ui/meeting/detail/` (9a·9b `CheckInDialogs.kt`, 9c `MeetingDetailScreen.kt`) |
| 8 | 알림 목록 | 3-6, 3-6-2 | `ui/notification/` |
| 9 | 마이페이지 | 14 | `ui/mypage/` |

## 4. 공통 (문찬주)

| 항목 | 프레임 | 폴더·파일 |
| --- | --- | --- |
| 로딩·에러·토스트 부품 | 16a~16l | `ui/common/` |
| 종목 기준 요약 행 | 5i | `ui/common/CriteriaSummaryRow.kt` |
| 하단 탭 바 | - | `ui/common/BottomNavBar.kt` |
| 화면 이동 | - | `ui/navigation/Routes.kt`, `AppNavHost.kt` |
| 공용 모델·Repository interface | - | `data/model/`, `data/repository/*Repository.kt` |
- 문진곤도 공용파일 제작에 관여 가능.

## 5. 흐름 A: 모임 운영 → 사후 관계 (문진곤)

호스트가 모임을 만들어 끝낼 때까지, 그리고 끝난 뒤의 관계를 다루는 흐름이다.

| 순서 | 기능 | 프레임 | 폴더 |
| --- | --- | --- | --- |
| 1 | 모임 개설(장소·시간·기준·설명) | 10a~10g-4 | `ui/meeting/create/` |
| 2 | 호스트 모임 상세·취소·삭제 | 5a-H 계열, 5a-X2, 5a-X2-2, 5a-X3 | `ui/meeting/detail/` (`HostSection.kt`, `MeetingCancelBottomSheet.kt`, `MeetingDialogs.kt`) |
| 3 | 신청 수락·거절·마감 | 7a~7d | `ui/meeting/manage/` |
| 4 | 공지·댓글 | 8a~8i | `ui/meeting/notice/` |
| 5 | 모임 종료 후 평가·매너온도 | 12a~12c-2 | `ui/mate/review/` |
| 6 | 메이트(목록·신청·프로필) | 11a~11c-2, 15a~15c | `ui/mate/` |
| 7 | 활동 내역 | 13-H, 13-H2, 13-P, 13-P2, 13-R | `ui/activity/` |

## 6. 겹치는 지점

| 지점 | 관련 | 정리 방법 |
| --- | --- | --- |
| 모임 상세 화면(5) | B·A | 화면 본체는 문찬주가 소유한다. 호스트 영역(5a-H, 취소·삭제)은 문진곤이 `ui/meeting/detail/` 안에 별도 파일로 만들어 끼운다. |
| 신청(B) → 수락(A) | B·A | `Meeting`, 신청 모델과 Repository interface를 먼저 같이 정한 뒤 각자 Fake로 개발한다. |
| 평가 대기 카드(13-R) → 평가(12) | A 내부 | 같은 담당이라 한 흐름으로 이어서 만든다. |
| 알림 목록(B) ← 공지·메이트 신청·체크인(A·B) | B·A | 알림 탭 이동 경로는 `Routes.kt`에 먼저 정의한다. |

## 7. 작업 순서 제안

1. 공통(문찬주): `UiState`, `Routes`, `AppNavHost`, 16번 부품, 공용 모델
2. 모델·Repository interface 합의(두 사람): `Meeting`, 신청, `User`, `Review`
3. 흐름 B(문찬주)와 흐름 A 1~4번(문진곤)을 병행
4. 흐름 A 5~7번(문진곤): 모임 종료 이후 기능이라 앞 단계 이후에 진행
5. 연결 점검(두 사람): 신청 → 수락, 알림 이동, 평가 대기 카드

## 8. 디자인에 없어서 확인할 것

- 문찬주: 6a·6b "한마디 없이 신청할까요?" 확인 프레임 없음, 14번 마이페이지의 '위치 설정'·'신고·차단 관리' 행(초기 범위 밖, 숨기거나 `TODO(임시)`).
- 문진곤: 호스트 ⋮ 메뉴(모임 수정·삭제·취소) 진입점 디자인 없음.
- 근거는 `docs/service-rules.md` 13장 "v4에서 새로 확인된 차이".

## 9. 진행 체크리스트

화면을 끝내면 `[ ]`를 `[x]`로 바꾼다. 하위 항목은 그 화면이 보여 주는 프레임(상태)이다. 폴더는 `app/src/main/java/com/example/myapplication/` 기준이다. 각자 자기 부분만 고친다.

### 문찬주 (흐름 B + 공통)

#### 0. 공통 (먼저 끝낼 것)

문진곤이 가져다 쓰므로 가장 먼저 한다. 공용 파일 수정은 자기 화면 작업과 섞지 않고 따로 커밋한다.

##### 계약 (모델·Repository·경로)
- [x] `data/model/` 모델과 상태 enum 확정 (`Meeting`, 신청, `User`, `Notice`, `Comment`, `Review`, `Notification`)
- [x] `data/repository/` interface 확정 (`MeetingRepository`, `UserRepository`)
- [x] `ui/navigation/Routes.kt` 전체 경로 목록
- [x] `ui/navigation/AppNavHost.kt` 경로와 화면 연결
- [ ] 문진곤과 모델·interface 합의 완료

##### 부품 (`ui/common/`)
- [x] `UiState.kt` 로딩/성공/에러
- [x] `BottomNavBar.kt` 탐색·활동·개설·메이트·마이
- [x] `SkeletonList.kt` ← 16a
- [x] `SkeletonDetail.kt` ← 16b
- [ ] `ActionButton.kt` ← 16c 제출 중, 16i 제출 실패
- [ ] `ListFooter.kt` ← 16d 추가 로딩, 16h 부분 실패
- [ ] `FullScreenError.kt` ← 16f 네트워크 없음, 16g 서버 오류
- [ ] `AppToast.kt` ← 16j 성공/경고/실패
- [ ] `CriteriaSummaryRow.kt` ← 5i 맞음/일부만/안 맞음
- [ ] `ConfirmDialog.kt` 다이얼로그 공통 틀

---

#### 1. 로그인 · 회원가입 · 종목 기록

##### `ui/auth/`
- [ ] `LoginScreen.kt`
  - [ ] 1 로그인 기본

##### `ui/record/`
- [ ] `SignUpInfoScreen.kt`
  - [ ] 2a 정보 입력
- [ ] `SignUpProfileScreen.kt`
  - [ ] 2b 프로필 설정
- [ ] `RecordScreen.kt`
  - [ ] 2c 헬스
  - [ ] 2c-2 헬스 하체·어깨
  - [ ] 2c-5 헬스 턱걸이 0회
  - [ ] 2d 러닝
  - [ ] 2e 등산
- [ ] `OneRmGuideBottomSheet.kt` ← 2c-3
- [ ] `PullUpGuideBottomSheet.kt` ← 2c-4
- [ ] `RecordState.kt`, `RecordViewModel.kt`

---

#### 2. 탐색 (지도·리스트)

##### `ui/explore/`
- [ ] `ExploreScreen.kt`
  - [ ] 3-After 지도 + 바텀시트
  - [ ] 3-2-After 지도 전체 (시트 내림)
  - [ ] 3-3-After 핀 선택
  - [ ] 3-5 같은 장소 모임 여러 개 (+n 핀 선택)
  - [ ] 16e 부분 로딩
- [ ] `ExploreBottomSheet.kt`
- [ ] `MapPin.kt` ← 3-4 핀 색 체계 (기본 vs 선택)
- [ ] `MeetingCard.kt`
- [ ] `ExploreState.kt`, `ExploreViewModel.kt`

---

#### 3. 조건 설정 (검색 필터)

##### `ui/explore/filter/`
- [ ] `FilterScreen.kt`
  - [ ] 4a 헬스
  - [ ] 4a-2 시간 범위 적용
  - [ ] 4a-3 기준 관계없음
  - [ ] 4b 러닝
  - [ ] 4c 등산
- [ ] `HealthFilterBottomSheet.kt`
  - [ ] 4d-After 헬스 기준
  - [ ] 4d-2 헬스 기준 꺼짐
  - [ ] 4d-3 부위 미선택 시
- [ ] `DetailConditionBottomSheet.kt` ← 4e 상세 조건
- [ ] `TimeRangeBottomSheet.kt` ← 4f 시간 범위 설정
- [ ] `LocationSearchScreen.kt`
  - [ ] 4g 위치 검색
  - [ ] 4g-2 검색 결과
- [ ] `FilterState.kt`, `FilterViewModel.kt`

---

#### 4. 모임 상세 (참가자 시점) · 신청 · 체크인

##### `ui/meeting/detail/`
- [ ] `MeetingDetailScreen.kt` 화면 본체 (소유: 문찬주)
  - [ ] 5a 모집중
  - [ ] 5g 헬스 모집중
  - [ ] 5l 신청 대기중
  - [ ] 5b 마감
  - [ ] 5c 진행중
  - [ ] 5d 종료
  - [ ] 호스트 영역 호출 지점 연결 (`isHost`일 때 문진곤의 호스트 composable 호출)
- [ ] `PlaceBottomSheet.kt` ← 5e 장소
- [ ] `CriteriaBottomSheet.kt`
  - [ ] 5f 등산 기준
  - [ ] 5h 헬스 기준
- [ ] `ApplyScreen.kt`
  - [ ] 6a 신청 작성 · 기준 일치
  - [ ] 6b 신청 작성 · 기준 미설정
- [ ] `CheckInDialogs.kt`
  - [ ] 9a 체크인 확인
  - [ ] 9b 체크인 확인 · 범위 밖
- [ ] 9c 진행중 · 체크인 완료 (`MeetingDetailScreen.kt` 안)
- [ ] 나가기(참가자) 동작 (`service-rules.md` 5번)
- [ ] `MeetingDetailState.kt`, `MeetingDetailViewModel.kt`

---

#### 5. 알림 · 마이페이지

##### `ui/notification/`
- [ ] `NotificationScreen.kt`
  - [ ] 3-6 알림 목록
  - [ ] 3-6-2 알림 목록 · 빈 상태
  - [ ] 탭 이동: 체크인 → 모임 상세 체크인 / 공지 → 공지 상세(8g) / 메이트 신청 → 메이트 신청(11c)

##### `ui/mypage/`
- [ ] `MyPageScreen.kt`
  - [ ] 14 마이페이지 기본

---

#### 6. 연결 점검 (문진곤과 함께)

- [ ] 신청(내 화면) → 수락·거절(문진곤 7번) 데이터가 이어지는지
- [ ] 알림 탭 이동 경로가 문진곤 화면(8g, 11c)으로 가는지
- [ ] 모임 상세에서 호스트 영역이 정상 표시되는지

### 문진곤 (흐름 A)

#### 1. 모임 개설

##### `ui/meeting/create/`
- [ ] `MeetingCreateScreen.kt`
  - [ ] 10a 헬스
  - [ ] 10a-2 헬스 기록 숨김
  - [ ] 10b 러닝
  - [ ] 10c 등산
- [ ] `HealthCriteriaBottomSheet.kt`
  - [ ] 10d-After 헬스 기준
  - [ ] 10d-2 헬스 기준 기록 숨김
- [ ] `DescriptionBottomSheet.kt` ← 10e 설명
- [ ] `TimeBottomSheet.kt` ← 10f 시간 설정
- [ ] `PlaceSearchScreen.kt`
  - [ ] 10g 장소 검색
  - [ ] 10g-2 검색 결과
- [ ] `PlaceConfirmScreen.kt`
  - [ ] 10g-3 장소 확인 · 헬스
  - [ ] 10g-4 집결지 지정 · 러닝
- [ ] `MeetingCreateState.kt`, `MeetingCreateViewModel.kt`
- [ ] 수정 동작 (`service-rules.md` 4번)

---

#### 2. 호스트 모임 상세 · 취소 · 삭제

##### `ui/meeting/detail/` (화면 본체는 문찬주 소유, 이 항목들은 별도 파일로 추가)
- [x] `HostSection.kt`
  - [x] 5a-H-After 모집중
  - [x] 5a-H2-After 신청자 없음
  - [x] 5a-H4-After 공지 2건
  - [x] 5a-H5 마감됨
- [x] `MeetingCancelBottomSheet.kt`
  - [x] 5a-X2 모임 취소 · 경고
  - [x] 5a-X2-2 사유 입력됨
- [x] `MeetingDialogs.kt` ← 5a-X3 모임 삭제 확인
- [ ] 문찬주의 `MeetingDetailScreen`과 호출 지점 연결 확인

---

#### 3. 참가자 관리 (수락·거절·마감)

##### `ui/meeting/manage/`
- [ ] `ParticipantManageScreen.kt`
  - [ ] 7a 신청 탭
  - [ ] 7b 확정 탭
  - [ ] 7c 신청 없음
  - [ ] 7d 마감 후
- [ ] `ApplicantsTab.kt`
- [ ] `ConfirmedTab.kt`
- [ ] `ParticipantManageState.kt`, `ParticipantManageViewModel.kt`
- [ ] 수락·거절·마감 동작 (`service-rules.md` 5번)

---

#### 4. 공지 · 댓글

##### `ui/meeting/notice/`
- [ ] `NoticeEditScreen.kt`
  - [ ] 8a 공지 작성 · 빈 상태
  - [ ] 8b 공지 작성 · 입력 중
  - [ ] 8c 공지 수정 · 기본
- [ ] `NoticeListScreen.kt`
  - [ ] 8d 공지 목록 · 호스트
  - [ ] 8e 공지 목록 · 참가자
- [ ] `NoticeDetailScreen.kt`
  - [ ] 8f 공지 상세 · 호스트
  - [ ] 8g 공지 상세 · 참가자
  - [ ] 8h 댓글 입력 중
  - [ ] 8i 댓글 없음

---

#### 5. 평가 · 매너온도

##### `ui/mate/review/`
- [ ] `ReviewListScreen.kt` ← 12a 목록
- [ ] `MemberReviewScreen.kt`
  - [ ] 12b-1 판정 미선택 (2/3)
  - [ ] 12b-2 좋음 선택 (2/3)
  - [ ] 12b-3 보통 선택 (2/3)
  - [ ] 12b-4 아쉬움 선택 (3/3 마지막)
- [ ] `ReviewDoneScreen.kt`
  - [ ] 12c 평가 완료
  - [ ] 12c-2 평가 완료 (추가 대상 없음)
- [ ] 매너온도 변화 규칙 (`service-rules.md` 11번)

---

#### 6. 메이트

##### `ui/mate/`
- [ ] `MateScreen.kt` (탭 틀)
- [ ] `MateListTab.kt`
  - [ ] 11a 메이트 탭
  - [ ] 11a-2 빈 상태
- [ ] `WorkedOutTab.kt`
  - [ ] 11b 함께 운동한 사람 탭
  - [ ] 11b-2 헬스 필터
  - [ ] 11b-3 필터 결과 빈 상태
- [ ] `MateRequestScreen.kt`
  - [ ] 11c 메이트 신청 2건
  - [ ] 11c-2 빈 상태
- [ ] `MateProfileScreen.kt`
  - [ ] 15a 메이트
  - [ ] 15b 메이트 아님
  - [ ] 15c 신청 보냄
- [ ] `MateState.kt`, `MateViewModel.kt`

---

#### 7. 활동 내역

##### `ui/activity/`
- [ ] `ActivityScreen.kt` (탭 틀)
- [ ] `HostActivityTab.kt`
  - [ ] 13-H 호스트 · 모임 6건 · 필터 노출
  - [ ] 13-H2 호스트 · 빈 상태
- [ ] `JoinedActivityTab.kt`
  - [ ] 13-P 참여 · 모임 3건 · 필터 숨김
  - [ ] 13-P2 참여 · 빈 상태
- [ ] `ReviewPendingCard.kt` ← 13-R 평가 대기 카드 (기한 비교)
- [ ] `ActivityState.kt`, `ActivityViewModel.kt`

---

#### 8. 연결 점검 (문찬주와 함께)

- [ ] 문찬주의 신청 데이터가 내 수락·거절 화면(7번)에 나오는지
- [ ] 알림 탭 이동이 내 공지 상세(8g)와 메이트 신청(11c)으로 가는지
- [ ] 활동 내역의 평가 대기 카드(13-R)가 평가 목록(12a)으로 이어지는지
- [ ] 호스트 영역이 문찬주의 모임 상세 안에 정상 표시되는지
