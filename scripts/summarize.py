import re
import sys
from transformers import GPT2LMHeadModel, GPT2Tokenizer


def download_model_and_tokenizer(model_name):
    # Download or load the model and tokenizer
    model = GPT2LMHeadModel.from_pretrained(model_name)
    tokenizer = GPT2Tokenizer.from_pretrained(model_name)
    return model, tokenizer


def summarize_text(model, tokenizer, text, max_new_tokens=150):
    # Tokenize the input text
    inputs = tokenizer.encode(text, return_tensors="pt", max_length=512, truncation=True)
    # Generate the summary
    summary_ids = model.generate(inputs, max_new_tokens=max_new_tokens, min_length=30, length_penalty=2.0,
                                 num_beams=4,
                                 early_stopping=True)
    # Decode the summary
    summary = tokenizer.decode(summary_ids[0], skip_special_tokens=True)
    return summary


def convert_lrc_to_markdown(lrc_string):
    # split input text into lines
    lines = lrc_string.strip().split('\n')

    # Remove header line
    lines = lines[1:]

    # delete timestamp and remove duplicates
    processed_lines = []
    last_line = ""
    for line in lines:
        # remove timestamp
        line_content = re.sub(r'\[\d{2}:\d{2}.\d{2}\]', '', line).strip()
        if line_content != last_line:
            processed_lines.append(f'- {line_content}')
            last_line = line_content

    return '\n'.join(processed_lines)


def main(model_name, input_file, output_file):
    # Download or load the model and tokenizer
    model, tokenizer = download_model_and_tokenizer(model_name)

    try:
        # Read the input file
        with open(input_file, "r", encoding="utf-8") as file:
            text = file.read()

        # Summarize the text
        summary = summarize_text(model, tokenizer, convert_lrc_to_markdown(text))

        # Write the summary to the output file
        with open(output_file, "w", encoding="utf-8") as file:
            file.write(summary)
    except UnicodeDecodeError as e:
        # Write the error message to the output file
        with open(output_file, "w", encoding="utf-8") as file:
            file.write(f"Failed to read the file due to encoding error: {e}")


if __name__ == "__main__":
    if len(sys.argv) != 3:
        print("Usage: python summarize.py <input_file> <output_file>")
        sys.exit(1)

    model_name = "gpt2"
    input_file = sys.argv[1]
    output_file = sys.argv[2]

    main(model_name, input_file, output_file)
