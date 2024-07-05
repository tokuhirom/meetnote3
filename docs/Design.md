# How it works?

## Overview

このアプリケーションは Mac 専用のアプリケーションとして開発されている。
ScreenCaptureKit の API を利用し、Zoom の Window が存在している場合に自動的に録音が開始される。
録音は、ディスプレイから出力される音声とマイクからの音声の両方を同時に行う。
2つの音声をミックスし、1つの音声ファイルとして保存する。
録音された音声を元に、whisper.cpp を用いて文字起こしを行う。
gpt2 を用いて、要約処理を行う。

## System architecture

Kotlin Native を採用する。

MeetNote2 では rust+tauri+svelte という構成だったが、Rust を採用すると RustRover を起動しなければならないのがネックだった。
ScreenCaptureKit などの Mac の API を簡単に利用出来るうえ、IntelliJ IDEA で開発が可能になり、Kotlin Native
での開発となると
通常業務からのコンテキストスイッチが容易であることから kotlin native での開発に切り替えることとした。

## 録音処理の詳細

録音処理においては、ScreenCaptureKit の API を利用して、ディスプレイから出力される音声とマイクからの音声の両方を同時に録音する。
ディスプレイから出力される音声は、ディスプレイから出力される音声を録音することで取得する。
マイクからの音声は、マイクからの音声を録音することで取得する。
2つの音声をミックスし、1つの音声ファイルとして保存する。出力結果は M4A としている。これは、Mac の API
で出力可能でロッシーな圧縮フォーマットであるためである。

## 文字起こし処理の詳細

whisper.cpp を用いて文字起こしを行う。
whisper.cpp は、音声ファイルを入力とし、音声ファイルの内容を文字起こしする。whisper.cpp は 16kHz の wave
ファイルを入力として受け付ける。
Mac の API でもこの処理は可能そうだが、やり方がわからないため、ffmpeg を用いて、音声を変換しています。

TODO:

whisper.cpp は lrc, vtt, srt の形式で出力することができる。Meetnote3 では lrc を採用している。

lrc の形式は以下の通りである。

```lrc
[by:whisper.cpp]
[00:00.00] Intro music
[00:15.30] First line of the song lyrics
[00:20.50] Second line of the song lyrics
[00:25.80] Third line of the song lyrics
[00:30.10] Fourth line of the song lyrics

[01:00.00] Chorus starts here
[01:05.30] First line of the chorus
[01:10.50] Second line of the chorus

[01:30.00] Second verse starts here
[01:35.30] First line of the second verse
[01:40.50] Second line of the second verse
```

vtt の形式は以下のようなものである。

```vtt
WEBVTT

00:00:00.000 --> 00:00:05.000
- Hello, welcome to our video.
- Today we'll be discussing WebVTT.

00:00:05.500 --> 00:00:10.000
- WebVTT stands for Web Video Text Tracks.
- It's used for subtitles and captions.

00:00:10.500 --> 00:00:15.000
- You can also use it for descriptions and chapters.
- Let's see an example.

00:00:15.500 --> 00:00:20.000
<v Speaker1>As you can see, this is a simple format.</v>
<v Speaker2>Yes, it's easy to use and implement.</v>

00:00:20.500 --> 00:00:25.000
<v Narrator>Thank you for watching.</v>
```

srt の形式は以下の通り。

```shell
1
00:00:00,000 --> 00:00:05,000
Hello, welcome to our video.
Today we'll be discussing SRT.

2
00:00:05,500 --> 00:00:10,000
SRT stands for SubRip Subtitle.
It's used for subtitles and captions.

3
00:00:10,500 --> 00:00:15,000
You can also use it for descriptions and chapters.
Let's see an example.

4
00:00:15,500 --> 00:00:20,000
As you can see, this is a simple format.
Yes, it's easy to use and implement.

5
00:00:20,500 --> 00:00:25,000
Thank you for watching.
```

lrc だと、一行の情報量が多いため、Meetnote3 では lrc を採用している。
パースも一行単位で行えば良いため、ハンドリングしやすいというメリットもあります。

### SFSpeechAudioBufferRecognitionRequest

Mac の Native API である SFSpeechAudioBufferRecognitionRequest を用いて文字起こしすることも可能です。
SFSpeechAudioBufferRecognitionRequest は通常ではリモートでの処理になりますが、MacOS Catalina 以後では、
requiresOnDeviceRecognition オプションを有効にすることでローカルでの音声文字起こしも可能になっているようです。

しかしながら、私のマシンでは動作しなかったため、採用を見送って、whisper.cpp を採用しています。

## 要約処理の詳細

whisper.cpp の出力結果は、lrc である。
要約処理の前段として、重複行の除外を行う。

重複業の除外を行った後、gpt2 を用いて要約処理を行う。
gpt2 以外にも様々な方法を試したが、gpt2 がローカルで動かせる中ではわりと精度たかくそれっぽい要約ができるため、一旦コレを採用している。

TODO: 将来的には pure kotlin で形態素解析器を作成し、それっぽい要約を行うことも検討している。

## UI の詳細

録音の開始や終了はすべて自動で行われるため、UI 上では操作は行われない。
UI は単なるビューワーである。

UI は kotlin native で Mac のネイティブコンポーネントを利用して実装されている。

この UI では、3種類の情報が表示可能となっている。

### Meeting Logs Window

ミーティングログを表示する機能。これが UI のコア機能である。

System Tray Icon にメニューが追加されており、"Open Meeting Logs" というメニューをクリックすると表示される。

Meeting Logs Window は、ログの内容を表示するためのウィンドウである。
過去のログがウィンドウの左側1/3 ぐらいの領域に一覧で表示される。ファイルの格納されているディレクトリ名をパースして、
録音した日時が表示されている。この領域をタップ/クリックすると右側2/3の領域にログの詳細情報が表示される。
この領域には、ミーティングの所要時間も表示されている。この時間は、LRC の最後の行のタイムスタンプから得られている。

ログの詳細情報表示領域には、音声ファイルの再生ボタン、要約結果、文字起こし結果が表示される。

### System Logs Window

アプリケーションの実行ログを表示することが可能となる。
アプリケーションのログの最近の5回分を表示可能。

Dropdown menu で直近5回分のログの名称が選択可能となっている。
Window は Tray Icon にメニューが追加されており、"Open System Logs" というメニューをクリックすると表示される。

現在実行中のログを表示することも可能となっている。

Dropdown menu の下には text を表示する枠があり、そこにログの内容が表示される。

### Stats Window

アプリケーションの統計情報を表示することが可能となる。

具体的には、現在の子プロセスの一覧、現在の whisper-cpp の稼働状況、サマライザの稼働状況、リカバリプロセスの稼働状況などがある。
