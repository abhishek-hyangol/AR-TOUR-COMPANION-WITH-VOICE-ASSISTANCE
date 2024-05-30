import torch
import pickle
import CustomTransformer as CT


max_seq_length= 100
# Load vocabulary
def take_model():
    embed_size = 256
    num_layers = 4
    forward_expansion = 4
    heads =4
    dropout = 0.083
    learning_rate = 0.01
    num_epochs = 50
    device=torch.device('cpu')
    with open('../model/NLP/model_2/model_2/vocab.pkl', 'rb') as f:
        vocab = pickle.load(f)
    
    # Load tokenizer
    with open('../model/NLP/model_2/model_2/tokenizer.pkl', 'rb') as f:
        tokenizer = pickle.load(f)
    
    # Create an instance of the Transformer model
    model = CT.Transformer(
        src_vocab_size=len(vocab.stoi),
        trg_vocab_size=len(vocab.stoi),
        src_pad_idx=vocab.stoi["<pad>"],
        trg_pad_idx=vocab.stoi["<pad>"],
        embed_size=embed_size,
        num_layers=num_layers,
        forward_expansion=forward_expansion,
        heads=heads,
        dropout=dropout,
        device=device
    ).to(device)
    # Load the saved model weights
    model.load_state_dict(torch.load('../model/NLP/model_2/model_2/transformer_model.pt', map_location=torch.device('cpu')))
    
    # Make sure to set the model in evaluation mode after loading the weights
    model.eval()
    return (model,tokenizer,vocab)
    
def infer_answer(question,max_seq_length=100, max_output_length=100):
    # Tokenize and numericalize the question
    model,tokenizer,vocab=take_model()
    question_tokens = tokenizer(question)
    question_indices = [vocab[token] for token in question_tokens]

    # Pad the question sequence
    padded_question_indices = question_indices[:max_seq_length] + [vocab['<pad>']] * (max_seq_length - len(question_indices))

    # Convert to tensor
    question_tensor = torch.tensor(padded_question_indices, dtype=torch.long).unsqueeze(0)  # Add batch dimension

    # Move tensor to device
    question_tensor = question_tensor.to(model.device)

    # Initialize the output sequence with start token
    trg_tensor = torch.tensor([vocab['<sos>']], dtype=torch.long).unsqueeze(0).to(model.device)  # Start token

    # Perform iterative decoding
    with torch.no_grad():
        model.eval()
        for _ in range(max_output_length):
            # Perform forward pass to predict the next token
            output = model(question_tensor, trg_tensor)

            # Get the predicted token (argmax)
            predicted_token = output.argmax(-1)[:,-1].item()

            # Append the predicted token to the output sequence
            trg_tensor = torch.cat((trg_tensor, torch.tensor([[predicted_token]]).to(model.device)), dim=-1)

            # Break if the model predicts an end-of-sequence token
            if predicted_token == vocab['<eos>']:
                break

    # Decode the output sequence into tokens
    predicted_answer_indices = trg_tensor.squeeze().tolist()

    # Remove start and end tokens from the predicted answer
    predicted_answer_indices = [idx for idx in predicted_answer_indices if idx not in [vocab['<sos>'], vocab['<eos>'], vocab['<pad>']]]

    # Convert indices to words
    predicted_answer_tokens = [vocab.itos[index] for index in predicted_answer_indices]

    # Convert tokens to string
    predicted_answer = ' '.join(predicted_answer_tokens)

    return predicted_answer