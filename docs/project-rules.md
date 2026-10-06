# 버디 고 프로젝트 규칙

이 문서는 버디 고 안드로이드 앱의 폴더 구조와 작업 규칙을 정리한 것이다.
새 파일을 만들거나 기존 코드를 고치기 전에 먼저 읽는다.

---

## 전체 폴더 구조

표기 설명

- `[화면]` 뒤로 가기로 돌아오는 독립된 화면
- `[탭]` 화면 안에서 갈아 끼워지는 탭 내용
- `[시트]` 화면 위에 뜨는 바텀시트
- `[다이얼로그]` 화면 위에 뜨는 확인창·메뉴
- `[부품]` 화면 안에서 쓰는 작은 조각
- `←` 뒤의 번호는 그 파일 하나에 들어가는 디자인 프레임 ID

```
app/
├─ ui/
│   ├─ navigation/                          화면 이동
│   │   ├─ Routes.kt                        화면 이름표 목록
│   │   └─ AppNavHost.kt                    이름표와 실제 화면을 연결
│   │
│   ├─ common/                              여러 화면이 같이 쓰는 것
│   │   ├─ UiState.kt                       로딩/성공/에러 정의
│   │   ├─ BottomNavBar.kt                  탐색·활동·개설·메이트·마이
│   │   ├─ SkeletonList.kt                  ← 12a
│   │   ├─ SkeletonDetail.kt                ← 12b
│   │   ├─ ActionButton.kt                  ← 12c 제출 중, 12i 제출 실패
│   │   ├─ ListFooter.kt                    ← 12d 추가 로딩, 12h 부분 실패
│   │   ├─ FullScreenError.kt               ← 12f 네트워크 없음, 12g 서버 오류
│   │   ├─ AppToast.kt                      ← 12j 성공/경고/실패
│   │   ├─ CriteriaSummaryRow.kt            ← 5i 맞음/일부만/안 맞음
│   │   └─ ConfirmDialog.kt                 다이얼로그 공통 틀
│   │
│   ├─ auth/                                ← 1번 온보딩 / 로그인
│   │   └─ LoginScreen.kt                   [화면] ← 1
│   │
│   ├─ record/                              ← 2번 종목 기록
│   │   ├─ SignUpInfoScreen.kt              [화면] ← 2a
│   │   ├─ SignUpProfileScreen.kt           [화면] ← 2b
│   │   ├─ RecordScreen.kt                  [화면] ← 2c, 2c-2, 2c-5, 2d, 2e
│   │   ├─ RecordState.kt
│   │   ├─ RecordViewModel.kt
│   │   ├─ OneRmGuideBottomSheet.kt         [시트] ← 2c-3
│   │   └─ PullUpGuideBottomSheet.kt        [시트] ← 2c-4
│   │
│   ├─ explore/                             ← 3번 탐색, 4번 조건 설정
│   │   ├─ ExploreScreen.kt                 [화면] ← 3 기본, 3-2 시트 내림,
│   │   │                                            3-3 핀 선택, 3-5 +n 핀 선택, 12e
│   │   ├─ ExploreState.kt                  시트 위치(기본 올림), 핀 선택
│   │   ├─ ExploreViewModel.kt
│   │   ├─ ExploreBottomSheet.kt            화면에 붙은 시트
│   │   │                                    ← 3 헤더+목록 / 3-2 헤더만 / 3-5 장소명+목록
│   │   ├─ MeetingCard.kt                   [부품] ← 목록의 카드, 3-3의 떠 있는 카드
│   │   ├─ MapPin.kt                        [부품] ← 3-4 기본/선택/비선택
│   │   └─ filter/
│   │       ├─ FilterScreen.kt              [화면] ← 4a, 4a-2, 4a-3, 4b, 4c
│   │       ├─ FilterState.kt
│   │       ├─ FilterViewModel.kt
│   │       ├─ HealthFilterBottomSheet.kt   [시트] ← 4d-After, 4d-2, 4d-2 부위 미선택
│   │       ├─ DetailConditionBottomSheet.kt [시트] ← 4e
│   │       ├─ TimeRangeBottomSheet.kt      [시트] ← 4f
│   │       └─ LocationSearchScreen.kt      [화면] ← 4g, 4g-2
│   │
│   ├─ meeting/                             ← 5번 모임 상세, 6번 모임 개설
│   │   ├─ detail/
│   │   │   ├─ MeetingDetailScreen.kt       [화면] ← 참가자 5a, 5g, 5l, 5b, 5c, 5c-3, 5d
│   │   │   │                                        호스트 5a-H, 5a-H2, 5a-H4, 5a-H13
│   │   │   ├─ MeetingDetailState.kt        진행 단계, 내 역할, 체크인 여부
│   │   │   ├─ MeetingDetailViewModel.kt
│   │   │   ├─ ApplicantsBottomSheet.kt     [시트] ← 5a-H3
│   │   │   ├─ PlaceBottomSheet.kt          [시트] ← 5e
│   │   │   ├─ CriteriaBottomSheet.kt       [시트] ← 5h 헬스, 5f 등산 (읽기 전용)
│   │   │   ├─ MeetingCancelBottomSheet.kt  [시트] ← 5a-H11, 5a-H11-2
│   │   │   ├─ MeetingDialogs.kt            [다이얼로그] ← 5a-H10, 5a-H12, 5c-2, 5c-2-2
│   │   │   └─ ApplyScreen.kt               [화면] ← 5j, 5k
│   │   ├─ notice/
│   │   │   ├─ NoticeEditScreen.kt          [화면] ← 5a-H5, 5a-H6, 5a-H7
│   │   │   └─ NoticeListScreen.kt          [화면] ← 5a-H8, 5a-H9
│   │   └─ create/
│   │       ├─ MeetingCreateScreen.kt       [화면] ← 6a, 6a-2, 6b, 6c
│   │       ├─ MeetingCreateState.kt
│   │       ├─ MeetingCreateViewModel.kt
│   │       ├─ HealthCriteriaBottomSheet.kt [시트] ← 6d, 6d-After, 6d-2, 6d-3, 기록 없음
│   │       ├─ DescriptionBottomSheet.kt    [시트] ← 6e
│   │       └─ TimeBottomSheet.kt           [시트] ← 6f
│   │
│   ├─ mate/                                ← 7번 메이트, 8번 메이트 평가, 11번 메이트 프로필
│   │   ├─ MateScreen.kt                    [화면] 제목 + 신청 배너 + 탭 바
│   │   ├─ MateState.kt                     선택된 탭, 신청 대기 수
│   │   ├─ MateViewModel.kt
│   │   ├─ MateListTab.kt                   [탭] ← 7a, 7a-2 빈 상태
│   │   ├─ WorkedOutTab.kt                  [탭] ← 7b, 7b-2 헬스 필터, 7b-3 빈 상태
│   │   ├─ MateRequestScreen.kt             [화면] ← 7c, 7c-2
│   │   ├─ MateProfileScreen.kt             [화면] ← 9a, 9b, 9c (11번 메이트 프로필)
│   │   └─ review/
│   │       ├─ ReviewListScreen.kt          [화면] ← 8a
│   │       ├─ MemberReviewScreen.kt        [화면] ← 8b 미선택/좋음/보통/아쉬움
│   │       └─ ReviewDoneScreen.kt          [화면] ← 8c, 8c-2
│   │
│   ├─ activity/                            ← 9번 활동 내역
│   │   ├─ ActivityScreen.kt                [화면] 제목 + 통계 + 평가 대기 카드 + 탭 바
│   │   ├─ ActivityState.kt                 선택된 탭, 종목 필터
│   │   ├─ ActivityViewModel.kt
│   │   ├─ HostActivityTab.kt               [탭] ← 9-H, 9-H2 빈 상태
│   │   ├─ JoinedActivityTab.kt             [탭] ← 9-P, 9-P2 빈 상태
│   │   └─ ReviewPendingCard.kt             [부품] ← 9-R 평소/마감 임박
│   │
│   └─ mypage/                              ← 10번 마이페이지
│       └─ MyPageScreen.kt                  [화면] ← 10
│
└─ data/
    ├─ model/                               앱에서 쓰는 데이터 모양
    │   ├─ Meeting.kt                       roleOf(userId) 포함
    │   ├─ User.kt
    │   ├─ Notice.kt
    │   └─ Review.kt
    ├─ repository/                          화면이 데이터를 요청하는 창구
    │   ├─ MeetingRepository.kt             interface
    │   ├─ FakeMeetingRepository.kt         목데이터로 응답
    │   ├─ RealMeetingRepository.kt         실제 API로 응답 (나중에)
    │   ├─ UserRepository.kt
    │   ├─ FakeUserRepository.kt
    │   └─ RealUserRepository.kt            (나중에)
    ├─ mock/                                목데이터
    │   ├─ MockMeetings.kt                  모집중/마감/진행중/종료/빈 목록
    │   └─ MockUsers.kt
    └─ api/                                 실제 서버 연결 (나중에)
        ├─ MeetingApi.kt
        └─ UserApi.kt
```

### 구조 요약

| 구분 | 개수 |
| --- | --- |
| 화면 | 20 |
| 탭 내용 | 4 |
| 바텀시트 | 12 (탐색의 붙박이 시트는 별도) |
| 다이얼로그 묶음 | 1 |

디자인의 12k(매트릭스)와 12l(규칙)은 코드가 아니라 문서여서 파일이 없다.

---

## 1. 프로젝트 개요

- 버디 고: 헬스·러닝·등산 모임을 지도 기반으로 찾고 개설하는 안드로이드 앱.
- 디자인 원본 파일의 이름은 `운동메이트 앱v3`이다.
- 화면을 가리킬 때는 디자인의 프레임 ID(5a, 6d 등)를 쓴다.

## 2. 폴더 구조

- `ui/`는 기능별 폴더(auth, record, explore, meeting, mate, activity, mypage)로 나눈다.
- 한 화면에 딸린 State, ViewModel, 바텀시트, 탭, 다이얼로그는 그 화면과 같은 폴더에 둔다.
- 두 개 이상의 기능 폴더에서 쓰는 것만 `ui/common/`에 둔다.
- 화면 이동 관련 코드는 `ui/navigation/`에 둔다.
- 데이터 관련 코드는 전부 `data/`(model, repository, mock, api)에 둔다.
- 전체 구조는 이 문서 위쪽의 전체 폴더 구조를 따른다.

## 3. 파일 이름

| 종류 | 접미사 | 예시 |
| --- | --- | --- |
| 화면 | `~Screen` | `MeetingDetailScreen.kt` |
| 상태 정의 | `~State` | `MeetingDetailState.kt` |
| 상태 변경 | `~ViewModel` | `MeetingDetailViewModel.kt` |
| 바텀시트 | `~BottomSheet` | `PlaceBottomSheet.kt` |
| 탭 내용 | `~Tab` | `MateListTab.kt` |
| 다이얼로그 | `~Dialog`, `~Dialogs` | `MeetingDialogs.kt` |

## 4. 화면 분류 기준

- 뒤로 가기로 돌아오는 이동이면 별도 Screen으로 만든다.
- 같은 틀 안에서 내용만 바뀌면 같은 Screen의 상태로 처리한다.
- 탭은 Screen 하나에 탭 내용 파일을 나눈다.
- 바텀시트와 다이얼로그는 화면이 아니다. 띄우는 화면과 같은 폴더에 파일로 둔다.
- 디자인 프레임 하나가 파일 하나를 뜻하지 않는다. 상태별 프레임은 한 파일에 들어간다.

## 5. 상태 관리

- State 파일은 값의 정의만 담고, 값을 바꾸는 코드는 ViewModel에만 둔다.
- Screen은 State를 받아 그리기만 하고, 직접 판단하거나 데이터를 불러오지 않는다.
- 버튼 글자나 활성화 여부처럼 다른 값에서 계산되는 것은 State 안에 계산 속성으로 둔다.
- 호스트/확정/대기/미신청 판단은 `Meeting.roleOf(userId)` 한 곳에서만 한다.
- 로딩·에러는 `common/UiState`를 쓴다.
- 상태가 거의 없는 단순한 화면은 State와 ViewModel 없이 Screen 파일 하나로 끝내도 된다.

## 6. 데이터

- 화면과 ViewModel은 Repository interface만 호출한다.
- 서버가 준비되기 전에는 `Fake~Repository`와 `mock/` 데이터를 쓴다.
- 목데이터에는 상태별 사례(모집중, 마감, 진행중, 종료, 빈 목록)를 모두 넣는다.
- 호스트 전용 동작은 화면에서 숨기는 것과 별개로 서버에서도 권한을 확인한다.

## 7. 라우팅

- 화면 이름은 `navigation/Routes.kt`에만 정의하고, 코드에서는 상수로만 쓴다.
- 화면 사이에는 ID(`meetingId`, `userId`)만 넘기고, 나머지 데이터는 도착한 화면이 불러온다.
- 바텀시트와 다이얼로그는 route로 만들지 않는다.
- 아직 안 만든 화면은 임시 화면으로 연결하고 `// TODO(임시)` 표시를 남긴다.

## 8. 공용 파일 수정

- 대상: `Routes.kt`, `AppNavHost.kt`, `ui/common/`, `data/model/`, Repository interface.
- 추가는 자유롭게 하되, 이름 변경과 삭제는 팀에 먼저 알린다.
- 공용 파일 수정은 자기 화면 작업과 섞지 않고 따로 커밋한다.

## 9. 담당 범위

- 문찬주: 모임 개설(`meeting/create/`), 내 모임 관리(호스트 관련 화면).
- 다른 팀원 담당 폴더는 고치기 전에 담당자에게 확인한다.
- 나머지 담당 배정은 팀에서 채워 넣는다.

## 10. 검토와 보고

- 기존 코드나 구조에서 잘못된 점을 발견하면 임의로 고치지 않고 먼저 보고한다.
- 보고할 때는 무엇이 문제인지, 어디에 있는지, 어떻게 고칠지를 함께 적는다.
- 디자인에 없는 동작은 추측해서 구현하지 않고 질문한다.
- 확인하지 않은 내용은 추측이라고 밝힌다.

## 11. 작업 방식

- 큰 변경은 계획을 먼저 보여주고 확인받은 뒤 단계별로 진행한다.
- 요청받지 않은 파일은 건드리지 않는다.
- 작업을 마치면 바꾼 파일과 확인한 내용을 짧게 정리한다.

## 12. 커밋

- 형식은 `종류: 내용 (프레임 ID)`로 쓴다.
    - 예: `feat: 모임 개설 화면 종목 선택 (6a, 6b, 6c)`
- 종류는 아래 다섯 가지를 쓴다.

| 종류 | 뜻 |
| --- | --- |
| `feat` | 기능 추가 |
| `fix` | 버그 수정 |
| `refactor` | 동작은 그대로 두고 구조만 변경 |
| `docs` | 문서 |
| `chore` | 설정, 빌드 |

- 한 커밋에는 한 가지 변경만 담는다.
- 브랜치는 `feature/기능명`으로 만들고, `main`에 직접 올리지 않는다.

## 13. 디자인

- 색, 간격, 모서리 값은 정해 둔 토큰을 쓰고 숫자를 직접 적지 않는다.
- 같은 모양의 부품이 이미 있으면 새로 만들지 않고 재사용한다.

## 14. PR (풀 리퀘스트)

- `main`으로 합치는 방법은 PR뿐이다. 브랜치는 `feature/기능명`으로 만든다. (12번)
- PR 하나에는 한 가지 기능이나 한 화면 묶음만 담는다. 공용 파일 수정은 따로 PR로 올린다. (8번)
- 제목은 커밋과 같은 형식인 `종류: 내용 (프레임 ID)`로 쓴다.
    - 예: `feat: 모임 개설 화면 종목 선택 (6a, 6b, 6c)`
- 본문에는 아래 네 가지를 적는다.

| 항목 | 내용 |
| --- | --- |
| 변경 내용 | 무엇을 바꿨는지 짧게 |
| 디자인 프레임 | 구현한 프레임 ID |
| 확인한 방법 | 빌드, 실행, 화면 확인 등 직접 해본 것 |
| 알릴 점 | 공용 파일 변경, 임시 화면(`TODO(임시)`), 디자인에 없어서 질문한 부분 |

- 본문은 아래 양식대로 쓴다. 해당 없는 항목은 지우지 않고 `해당 없음`으로 적는다.

```
## 변경 내용
- 프로젝트 규칙 문서(docs/project-rules.md) 추가
- 규칙 문서의 폴더 구조대로 ui/, data/ 아래 Kotlin 뼈대 파일 생성

## 디자인 프레임
해당 없음

## 확인한 방법
- compileDebugKotlin 빌드 통과

## 알릴 점
- 화면, 탭, 시트는 // TODO(임시)가 붙은 빈 Composable입니다.
- .idea/는 제외했습니다.
```

- 올리기 전에 빌드가 통과하는지 확인한다. CI(빌드, 단위 테스트, lint)가 통과해야 합친다.
- 다른 팀원 담당 폴더를 고쳤다면 그 담당자를 리뷰어로 지정한다. (9번)
- 리뷰어는 최소 1명의 승인을 받은 뒤에 합친다. 본인이 올린 PR은 본인이 승인하지 않는다.
- 리뷰에서 받은 지적은 같은 브랜치에 커밋을 추가해서 고친다.
- 합친 뒤에는 작업 브랜치를 삭제한다.
- 요청받지 않은 파일은 PR에 넣지 않는다. (11번)
