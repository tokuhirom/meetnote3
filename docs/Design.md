# How it works?

## Overview

このアプリケーションは Mac 専用のアプリケーションとして開発されている。
ScreenCaptureKit の API を利用し、Zoom の Window が存在している場合に自動的に録音が開始される。
録音は、ディスプレイから出力される音声とマイクからの音声の両方を同時に行う。
2つの音声をミックスし、1つの音声ファイルとして保存する。
録音された音声を元に、whisper.cpp を用いて文字起こしを行う。
gpt2 を用いて、要約処理を行う。

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
SFSpeechAudioBufferRecognitionRequest を用いて、Mac の API で変換する方法もあるかもしれません。
SFSpeechAudioBufferRecognitionRequest は通常ではリモートでの処理になりますが、MacOS Catalina 以後では、
requiresOnDeviceRecognition オプションを有効にすることでローカルでの音声文字起こしも可能になっているようです。

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

## 要約処理の詳細

whisper.cpp の出力結果は、lrc である。
要約処理の前段として、重複行の除外を行う。

## UI の詳細

録音の開始や終了はすべて自動で行われるため、UI 上では操作は行われない。
UI は単なるビューワーである。
