import unittest
from unittest.mock import patch, mock_open, MagicMock

from summarize import download_model_and_tokenizer, summarize_text, main


class TestSummarize(unittest.TestCase):

    @patch('summarize.GPT2LMHeadModel.from_pretrained')
    @patch('summarize.GPT2Tokenizer.from_pretrained')
    def test_download_model_and_tokenizer(self, mock_model, mock_tokenizer):
        model_name = "gpt2"
        model, tokenizer = download_model_and_tokenizer(model_name)
        mock_model.assert_called_once_with(model_name)
        mock_tokenizer.assert_called_once_with(model_name)

    @patch('summarize.GPT2Tokenizer.encode', return_value=[0])
    @patch('summarize.GPT2LMHeadModel.generate', return_value=[[0]])
    @patch('summarize.GPT2Tokenizer.decode', return_value="summary text")
    def test_summarize_text(self, mock_decode, mock_generate, mock_encode):
        # Set up the mocks
        mock_model = MagicMock()
        mock_tokenizer = MagicMock()

        # Mock the methods on the tokenizer
        mock_tokenizer.encode.return_value = [0]
        mock_tokenizer.decode.return_value = "summary text"

        # Mock the generate method on the model
        mock_model.generate.return_value = [[0]]

        text = "Some long text"
        summary = summarize_text(mock_model, mock_tokenizer, text)
        self.assertEqual(summary, "summary text")

    @patch("builtins.open", new_callable=mock_open, read_data="Some input text")
    @patch("summarize.summarize_text", return_value="summary text")
    @patch('summarize.download_model_and_tokenizer')
    def test_main(self, mock_download_model_and_tokenizer, mock_summarize_text, mock_open):
        mock_model = patch('summarize.GPT2LMHeadModel').start().return_value
        mock_tokenizer = patch('summarize.GPT2Tokenizer').start().return_value
        mock_download_model_and_tokenizer.return_value = (mock_model, mock_tokenizer)

        model_name = "gpt2"
        input_file = "input.txt"
        output_file = "output.txt"

        main(model_name, input_file, output_file)

        # Check that the input file was opened correctly
        mock_open.assert_any_call(input_file, "r", encoding="utf-8")
        # Check that the output file was written correctly
        mock_open().write.assert_called_with("summary text")


if __name__ == "__main__":
    unittest.main()
