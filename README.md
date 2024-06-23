Here's the updated README with the requested additions:

# Meetnote3

## Overview

Meetnote3 is a powerful and convenient tool designed to run on macOS 14.4.1+ Arm64. It utilizes screencapturekit
and is written in Kotlin Native, providing seamless integration and performance. This project is the successor
to [Meetnote2](https://github.com/tokuhirom/meetnote2).

## Features

- **Automatic Recording**: Automatically records audio when a Zoom window is detected.
- **AI Transcription**: Transcribes the recorded audio using advanced AI technology.
- **AI-Powered Summarization**: Generates concise summaries from the transcriptions using cutting-edge AI,
  saving you time and effort.

## Installation

To set up Meetnote3, you need to install dependencies using Homebrew. While it's not the simplest setup, it's
straightforward enough for most users.

```shell
brew install whisper-cpp ffmpeg
```

(If you don't have Homebrew installed, you can find instructions [here](https://brew.sh/).)

After installing the dependencies, you can download the binary from the **GitHub releases page**(Not available
yet)

## License

```
The MIT License (MIT)

Copyright © 2024 Tokuhiro Matsuno, http://64p.org/ <tokuhirom@gmail.com>

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the “Software”), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
```

## Development Status

Meetnote3 is currently under active development. We are actively seeking feedback to improve the tool. Please
feel free to contribute your thoughts and suggestions!

Stay tuned for more features and improvements!
