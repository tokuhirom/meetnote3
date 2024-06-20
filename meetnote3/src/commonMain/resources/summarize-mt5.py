import sys
from transformers import MT5ForConditionalGeneration, MT5Tokenizer


def download_model_and_tokenizer(model_name):
    # Download or load the model and tokenizer
    model = MT5ForConditionalGeneration.from_pretrained(model_name)
    tokenizer = MT5Tokenizer.from_pretrained(model_name, legacy=False)
    return model, tokenizer


def summarize_text(model, tokenizer, text, max_length=150):
    # Tokenize the input text
    inputs = tokenizer.encode("summarize: " + text, return_tensors="pt", max_length=512, truncation=True)
    # Generate the summary
    summary_ids = model.generate(inputs, max_length=max_length, min_length=30, length_penalty=2.0, num_beams=4,
                                 early_stopping=True)
    # Decode the summary
    summary = tokenizer.decode(summary_ids[0], skip_special_tokens=True)
    return summary


def main(model_name, input_file, output_file):
    # Download or load the model and tokenizer
    model, tokenizer = download_model_and_tokenizer(model_name)

    # Read the input file
    with open(input_file, "r", encoding="utf-8") as file:
        text = file.read()

    # Divide the text into chunks
    chunk_size = 512  # 512トークンごとに分割
    chunks = [text[i:i + chunk_size] for i in range(0, len(text), chunk_size)]

    # Summarize each chunk
    summaries = []
    for chunk in chunks:
        summary = summarize_text(model, tokenizer, chunk)
        summaries.append(summary)

    # Join the summaries
    full_summary = "\n".join(summaries)

    # Write the summary to the output file
    with open(output_file, "w", encoding="utf-8") as file:
        file.write(full_summary)


if __name__ == "__main__":
    if len(sys.argv) != 4:
        print("Usage: python summarize.py <model_name> <input_file> <output_file>")
        print(
            "model_name should be either 'tsmatz/mt5_summarize_japanese' or 'csebuetnlp/mT5_multilingual_XLSum'")
        sys.exit(1)

    model_name = sys.argv[1]
    input_file = sys.argv[2]
    output_file = sys.argv[3]

    if model_name not in ["tsmatz/mt5_summarize_japanese", "csebuetnlp/mT5_multilingual_XLSum"]:
        print(
            "Invalid model_name. Choose 'tsmatz/mt5_summarize_japanese' or 'csebuetnlp/mT5_multilingual_XLSum'")
        sys.exit(1)

    main(model_name, input_file, output_file)
