| Release Builds | Dint Support Server |
|-------|----------|
| [![stable release](https://img.shields.io/github/release/OncePunchedMan/DINT.svg?maxAge=3600&label=download)](https://github.com/OncePunchedMan/DINT/releases/latest) | [![Discord](https://img.shields.io/discord/1195734228319617024.svg?label=discord&labelColor=7289da&color=2c2f33&style=flat)](TBD) |

# ![app icon](./.github/readme-images/app-icon.png) Dint — Do I Need To?

Do you find yourself pointlessly unlocking your phone, thinking to yourself: "Hey, why did I just do that?" That's why I created Dint. It makes you pause and asks you why you just unlocked your phone. 

It's not a screen-time timer. It's not a blocklist. It's a tiny, mildly annoying app that asks you to justify yourself before you doomscroll/stare at some Youtube vid type shit. Sometimes the honest answer is "I need to reply to my mom" and it lets you straight through. If you're honest with yourself and truly answer with "I'm bored", then you're better off keeping that phone locked.

## Why this exists

Most screen-time tools step in *after* you're already using your phone. A timer runs out, a blocklist kicks in, you get nagged mid-scroll. By then the habit already won. Dint steps in at the actual decision point: the moment you unlock.

The goal is not guilt or shame. It's just making the moment conscious enough that *you* get to choose what happens next, instead of your boredom or habit deciding for you.

I'm not gonna lie, I'm no exception to this. Many times even after installing and using Dint, I saw myself skipping the prompt, but honestly, you're only cheating yourself. Be in control when unlocking your phone. Live in the moment instead of scrolling reel after reel. At least that's how it was for me. Screen time always felt natural, without even really needing it. Time enjoying the world, though, is already limited and too short to waste on a tiny screen for a few seconds of dopamine. 
There are similar apps to this already in the app store or wherever. They might be prettier than this one, but mine is totally free. No subscription whatsoever, no one-time payment. Simply enjoy your free time.

## What it actually does

- **Catches you right after unlock.** An accessibility service watches for the post-unlock window and, if you open something within a few seconds, shows the prompt before you can act.
- **Makes you say why.** Pick a curated reason (or type your own) — "reply to someone," "check directions," "I'm bored," whatever's true. Some reasons let you through immediately; some relock the phone instead.
- **Adjustable friction.** A slider controls how long you have to sit with the decision. It goes from a light, instant nudge up to a proper "no, actually wait a few seconds".
- **Scheduling.** Only get nudged during the hours you actually want nudging (say, 9-to-5 doomscroll-prevention, off on weekends, whatever).
- **Hard mode**, for when you don't trust yourself to back out gracefully. You have to make an actual choice.
- **Hidden lock outcomes**, so the "keep me locked" options aren't visually distinguishable from the "let me through" ones.
- **Preset app shortcuts** for your usual doomscroll targets (memes, social, video, shopping...) so continuing takes you exactly where you meant to go. You can choose the apps you want the preset to open.
- **Excluded apps**, with common banking/wallet apps excluded by default, plus your own list.
- **A history**, daily unlock counts, how many times you continued vs. stayed locked, stored on-device via Room. No analytics, no server, nobody's phone-habit-shaming you but you.

## Tech stack

Kotlin + Jetpack Compose, Material3 1.4.0, Room for local history, WorkManager for background update checks, KSP for annotation processing, AGP 9.3.1 / Gradle 9.7.1 / Kotlin 2.4.10 on the build side. `minSdk` 29 (Android 10+), because supporting ancient Android versions isn't something one should do.

No Play Store: Updates ship via GitHub Releases and the app checks for and installs its own updates (with your permission, obviously).

## Getting it running

1. Open the project in Android Studio (or just `./gradlew` it from a terminal (why though)).
2. Let Gradle sync.
3. Install it on a device or emulator.
4. In the app's onboarding, grant the accessibility service.
5. Optionally grant device admin if you want the "lock it back right now" button to actually do something.
6. Optionally disable battery optimization for it and allow notifications, so Android doesn't quietly kill the service in the background.

## The honest disclaimer

This is a personal project, and yes a lot of it was built with an AI doing the typing while I did the deciding. I'm not a Kotlin novice hiding behind Claude and others; I read the diffs, I understand what's going on in here, and I can (and do) fix things myself when something's off. But I'm not gonna pretend this started life as a meticulously hand-crafted app. It started as "I want my phone to ask me why and don't wanna have to pay for premium features," and grew from there.

Realistically, maybe three people other than me will ever install this. That's fine. It's built for how *I* want to use my phone. But if you're one of those three (hi!) and you've got an idea that makes sense for more people than just us, I'm genuinely happy to look at it.

## Contributing

- Found a bug? Open an issue. Make sure to actually provide context and explanations of the issue you're facing.
- Want to add something? Cool, but keep the spirit intact: this app is supposed to feel calm and a little reflective, not like another naggy productivity app with streaks and guilt notifications. If your feature idea fights that vibe, it's probably not going to land here even if it's well-built.
- PRs: keep them focused. One idea per PR beats a 40-file "also I refactored the theme" surprise.

## License

AGPL-3.0. Fork it, modify it, use it. Just know that if you distribute a modified version (including running it as a hosted service), you have to share your source too. See the [LICENSE](LICENSE) file for the full text.
