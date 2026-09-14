# Android Project Instructions

# IMPORTANT
keep things clean simple and effective, no over-engineering  

This is a native Android application.

## Tech stack

- Language: Kotlin
- UI: XML layouts
- DO NOT use Jetpack Compose
- Use Android Views
- Prefer ConstraintLayout for complex screens
- Use LinearLayout only for simple layouts
- Use RecyclerView for lists
- Use CardView or MaterialCardView for cards
- Use ViewBinding for accessing views
- Use Material Components where appropriate

## UI implementation

When implementing designs from Figma:

- Convert Figma layouts into Android XML.
- Do not generate React, HTML, CSS, Tailwind or Compose.
- Put layouts in res/layout.
- Put colors in res/values/colors.xml.
- Put dimensions in res/values/dimens.xml when reused.
- Put strings in res/values/strings.xml.
- Put icons/images in res/drawable.
- Avoid hardcoded strings.
- Avoid unnecessary hardcoded dimensions.
- Prefer ConstraintLayout over deeply nested LinearLayouts.
- Follow the Figma spacing, typography, radius and colors as closely as possible.

## Naming

Use descriptive snake_case resource IDs:

task_title
task_description
task_card
btn_add_task
rv_task_groups

Layout names:

activity_main.xml
activity_create_task.xml
activity_task_detail.xml

RecyclerView items:

item_task.xml
item_task_group.xml

## Architecture

Activities should contain UI coordination logic.

RecyclerView should use:
- Adapter
- ViewHolder
- XML item layouts

Do not introduce Jetpack Compose unless explicitly requested.

- Do not run a full Android/Gradle build after every code change.
- Prefer static inspection, IDE diagnostics, and targeted checks while iterating.
- Only run a Gradle build when:
  - a meaningful feature or screen is completed,
  - changes affect multiple modules or dependencies,
  - Gradle/resource errors need verification,
  - or I explicitly request a build.

- Prefer lightweight/targeted commands over full builds when possible.
- Avoid repeatedly running:
  ./gradlew build
  ./gradlew clean build

- Never run `clean` unless necessary to diagnose a Gradle/cache issue.

- For normal verification, prefer:
  ./gradlew :app:compileDebugKotlin
  ./gradlew :app:lintDebug

- Use:
  ./gradlew :app:assembleDebug
  only when an APK or full debug compilation is actually needed.

- Batch multiple related code changes before running Gradle verification.