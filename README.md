# 🎹 Piano Academy Android

React 웹앱을 **Kotlin + Jetpack Compose** 네이티브 Android 앱으로 변환한 프로젝트

---

## 프로젝트 구조

```
PianoAcademy-Android/
├── app/
│   ├── src/main/
│   │   ├── java/com/pianoacademy/
│   │   │   ├── MainActivity.kt              ← 앱 진입점
│   │   │   ├── data/
│   │   │   │   ├── PianoData.kt            ← 음표 정의, 주파수, 파싱
│   │   │   │   └── SongData.kt             ← 105곡 데이터 (7단계)
│   │   │   ├── audio/
│   │   │   │   └── PianoSoundEngine.kt     ← AudioTrack 합성음 엔진
│   │   │   ├── viewmodel/
│   │   │   │   └── PianoViewModel.kt       ← 앱 상태 + 재생 로직
│   │   │   └── ui/
│   │   │       ├── theme/Theme.kt          ← 다크 테마 + 색상
│   │   │       ├── PianoScreen.kt          ← 메인 화면 레이아웃
│   │   │       ├── PianoKeyboard.kt        ← 멀티터치 건반
│   │   │       ├── FallingNotesView.kt     ← 폭포수 뷰
│   │   │       ├── SheetMusicView.kt       ← 악보 뷰
│   │   │       └── components/
│   │   │           ├── TopBar.kt           ← 상단 컨트롤
│   │   │           ├── SongPanel.kt        ← 곡 목록
│   │   │           └── GameResultDialog.kt ← 결과 팝업
│   │   ├── res/values/
│   │   │   ├── strings.xml
│   │   │   └── themes.xml
│   │   └── AndroidManifest.xml
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── gradle/libs.versions.toml              ← 의존성 버전 관리
├── settings.gradle.kts
└── build.gradle.kts
```

---

## 개발 환경 설정

### 요구사항
- Android Studio Ladybug (2024.2.1) 이상
- JDK 17
- Android SDK API 35 (compileSdk)
- minSdk 26 (Android 8.0)

### 빌드 방법
```bash
# 1. GitHub에서 clone
git clone https://github.com/your-repo/PianoAcademy-Android.git

# 2. Android Studio로 열기
# File → Open → PianoAcademy-Android 폴더 선택

# 3. Gradle Sync
# (자동으로 진행됨)

# 4. 실행
# Run → Run 'app' (Shift+F10)
```

---

## 주요 기능

| 기능 | 상태 |
|------|------|
| 피아노 건반 34건반 (C3~B5) | ✅ |
| 멀티터치 지원 | ✅ |
| 7단계 난이도 시스템 | ✅ |
| 105곡 (동요/대중가요/클래식/K-POP/명곡) | ✅ |
| 자동재생 모드 | ✅ |
| 따라하기 모드 + 정확도 측정 | ✅ |
| 3성 별점 시스템 | ✅ |
| 폭포수 뷰 (UP/DOWN) | ✅ |
| 악보 뷰 | ✅ |
| 음색 모드 5가지 (그랜드/일렉/소프트/비브라폰/오르간) | ✅ |
| 가로/세로 화면 자동 전환 | ✅ |
| 볼륨/템포 조절 | ✅ |
| 최고점수 저장 | ✅ |

---

## Play Store 출시 전 추가 작업

### 필수
- [ ] `ic_launcher.png` 아이콘 제작 (512×512 등 다해상도)
- [ ] 개인정보처리방침 URL 등록
- [ ] Keystore 파일 생성
- [ ] AAB 빌드 및 서명

### 권장
- [ ] 오디오 레이턴시 최적화 (AudioTrack → Oboe 라이브러리)
- [ ] 햅틱 피드백 추가
- [ ] 점수 SharedPreferences 영구 저장
- [ ] 광고 (AdMob) 통합
- [ ] 인앱결제 (3~7단계 잠금해제)

---

## 기술 스택

- **언어**: Kotlin 2.0
- **UI**: Jetpack Compose (Material3)
- **아키텍처**: MVVM + StateFlow
- **오디오**: Android AudioTrack (합성음)
- **최소 SDK**: Android 8.0 (API 26)
- **빌드 시스템**: Gradle KTS
