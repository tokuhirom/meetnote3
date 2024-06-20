import sys
from transformers import BartForConditionalGeneration, BartTokenizer


def download_model_and_tokenizer(model_name):
    # Download or load the model and tokenizer
    model = BartForConditionalGeneration.from_pretrained(model_name)
    tokenizer = BartTokenizer.from_pretrained(model_name)
    return model, tokenizer


def summarize_text(model, tokenizer, text):
    # Tokenize the input text
    inputs = tokenizer.encode("summarize: " + text, return_tensors="pt", max_length=1024, truncation=True)
    # Generate the summary
    summary_ids = model.generate(inputs, max_length=150, min_length=30, length_penalty=2.0, num_beams=4,
                                 early_stopping=True)
    # Decode the summary
    summary = tokenizer.decode(summary_ids[0], skip_special_tokens=True)
    return summary


def main(input_file, output_file):
    model_name = "facebook/bart-large-cnn"

    # Download or load the model and tokenizer
    model, tokenizer = download_model_and_tokenizer(model_name)

    # Read the input file
    with open(input_file, "r", encoding="utf-8") as file:
        text = file.read()

    # Generate the summary
    summary = summarize_text(model, tokenizer, text)

    # Write the summary to the output file
    with open(output_file, "w", encoding="utf-8") as file:
        file.write(summary)


if __name__ == "__main__":
    if len(sys.argv) != 3:
        print("Usage: python summarize.py <input_file> <output_file>")
        sys.exit(1)

    input_file = sys.argv[1]
    output_file = sys.argv[2]

    main(input_file, output_file)
