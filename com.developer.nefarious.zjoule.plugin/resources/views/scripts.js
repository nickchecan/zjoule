const loadingIcon = `
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100" preserveAspectRatio="xMidYMid" width="50" height="50" style="shape-rendering: auto; display: block; background: none;" xmlns:xlink="http://www.w3.org/1999/xlink">
  <g>
    <circle fill="#422e78" r="10" cy="50" cx="84">
      <animate begin="0s" keySplines="0 0.5 0.5 1" values="10;0" keyTimes="0;1" calcMode="spline" dur="0.25s" repeatCount="indefinite" attributeName="r"></animate>
      <animate begin="0s" values="#422e78;#d5c5ff;#a785ff;#8c60ff;#422e78" keyTimes="0;0.25;0.5;0.75;1" calcMode="discrete" dur="1s" repeatCount="indefinite" attributeName="fill"></animate>
    </circle>
    <circle fill="#422e78" r="10" cy="50" cx="16">
      <animate begin="0s" keySplines="0 0.5 0.5 1;0 0.5 0.5 1;0 0.5 0.5 1;0 0.5 0.5 1" values="0;0;10;10;10" keyTimes="0;0.25;0.5;0.75;1" calcMode="spline" dur="1s" repeatCount="indefinite" attributeName="r"></animate>
      <animate begin="0s" keySplines="0 0.5 0.5 1;0 0.5 0.5 1;0 0.5 0.5 1;0 0.5 0.5 1" values="16;16;16;50;84" keyTimes="0;0.25;0.5;0.75;1" calcMode="spline" dur="1s" repeatCount="indefinite" attributeName="cx"></animate>
    </circle>
    <circle fill="#8c60ff" r="10" cy="50" cx="50">
      <animate begin="-0.25s" keySplines="0 0.5 0.5 1;0 0.5 0.5 1;0 0.5 0.5 1;0 0.5 0.5 1" values="0;0;10;10;10" keyTimes="0;0.25;0.5;0.75;1" calcMode="spline" dur="1s" repeatCount="indefinite" attributeName="r"></animate>
      <animate begin="-0.25s" keySplines="0 0.5 0.5 1;0 0.5 0.5 1;0 0.5 0.5 1;0 0.5 0.5 1" values="16;16;16;50;84" keyTimes="0;0.25;0.5;0.75;1" calcMode="spline" dur="1s" repeatCount="indefinite" attributeName="cx"></animate>
    </circle>
    <circle fill="#a785ff" r="10" cy="50" cx="84">
      <animate begin="-0.5s" keySplines="0 0.5 0.5 1;0 0.5 0.5 1;0 0.5 0.5 1;0 0.5 0.5 1" values="0;0;10;10;10" keyTimes="0;0.25;0.5;0.75;1" calcMode="spline" dur="1s" repeatCount="indefinite" attributeName="r"></animate>
      <animate begin="-0.5s" keySplines="0 0.5 0.5 1;0 0.5 0.5 1;0 0.5 0.5 1;0 0.5 0.5 1" values="16;16;16;50;84" keyTimes="0;0.25;0.5;0.75;1" calcMode="spline" dur="1s" repeatCount="indefinite" attributeName="cx"></animate>
    </circle>
    <circle fill="#d5c5ff" r="10" cy="50" cx="16">
      <animate begin="-0.75s" keySplines="0 0.5 0.5 1;0 0.5 0.5 1;0 0.5 0.5 1;0 0.5 0.5 1" values="0;0;10;10;10" keyTimes="0;0.25;0.5;0.75;1" calcMode="spline" dur="1s" repeatCount="indefinite" attributeName="r"></animate>
      <animate begin="-0.75s" keySplines="0 0.5 0.5 1;0 0.5 0.5 1;0 0.5 0.5 1;0 0.5 0.5 1" values="16;16;16;50;84" keyTimes="0;0.25;0.5;0.75;1" calcMode="spline" dur="1s" repeatCount="indefinite" attributeName="cx"></animate>
    </circle>
  </g>
</svg>
`

function sendMessage() {
	const userInput = document.getElementById('userInput').value;
	if (userInput.trim() === "") return;
	try {
		addUserMessage(userInput);
		getAIResponse(userInput); // Java callback
		startLoading();
	} catch (e) {
	}
	updatePlaceholder();
	clearMessageAndScrollDown();
}

function addUserMessage(input) {
	const chatBox = document.getElementById('chatBox');
	const message = document.createElement('div');
	message.className = 'message user-message';
	message.textContent = input;
	chatBox.appendChild(message);
}

function addBotMessage(input) {
	const chatBox = document.getElementById('chatBox');
	const message = document.createElement('div');
	message.className = 'message bot-message';
	message.innerHTML = marked.parse(input);
	chatBox.appendChild(message);
}

function clearMessageAndScrollDown() {
	const chatBox = document.getElementById('chatBox');
	document.getElementById('userInput').value = "";
	chatBox.scrollTop = chatBox.scrollHeight;
}

function receiveMessage(output) {
	finishLoading();
	addBotMessage(output);
	updatePlaceholder();
	clearMessageAndScrollDown();
}

function login() {
	hideInstructions();
	activateChat();
	updatePlaceholder();
}

function logout() {
	clearMessages();
	deactivateChat();
	showInstructions();
}

function clearMessages() {
	const chatBox = document.getElementById('chatBox');
	chatBox.innerHTML = ''; // Remove all child elements
}

function deactivateChat() {
	const chatContainer = document.querySelector(".chat-container");
	chatContainer.style.display = "none";
}

function activateChat() {
	const chatContainer = document.querySelector(".chat-container");
	chatContainer.style.display = 'flex'; // Show the input
}

function showInstructions() {
	hidePlaceholder();
	const instructions = document.getElementById("instructions");
	instructions.style.display = "block";
}

function hideInstructions() {
	const instructions = document.getElementById("instructions");
	instructions.style.display = "none";
}

function updateTag(file) {
	const tagBox = document.querySelector(".tag-box");
	if (file != 'null') {
		tagBox.textContent = file;
		tagBox.style.display = "block";
	} else {
		tagBox.textContent = '';
		tagBox.style.display = "none";
	}
}

function updatePlaceholder() {
	const chatBox = document.getElementById("chatBox");
	const hasMessages = !!chatBox.querySelector(".message");

	if (hasMessages) {
		hidePlaceholder();
	} else {
		showPlaceholder();
	}
}

function showPlaceholder() {
	const placeholder = document.getElementById("placeholder");
	placeholder.style.display = "block";
}

function hidePlaceholder() {
	const placeholder = document.getElementById("placeholder");
	placeholder.style.display = "none";
}

let streamingText = "";

function blockInput() {
	document.getElementById('userInput').disabled = true;
	document.getElementById('send').disabled = true;
	const stop = document.getElementById('stop');
	if (stop) {
		stop.disabled = false;
	}
}

function unblockInput() {
	document.getElementById('userInput').disabled = false;
	document.getElementById('send').disabled = false;
	const stop = document.getElementById('stop');
	if (stop) {
		stop.disabled = true;
	}
}

function startLoading() {
	const chatBox = document.getElementById('chatBox');
	const message = document.createElement('div');
	message.className = 'message bot-message';
	message.innerHTML = loadingIcon;
	chatBox.appendChild(message);
	blockInput();
}

function finishLoading() {
	removeLoading();
	unblockInput();
}

function removeLoading() {
	const chatBox = document.getElementById('chatBox');
	const botMessages = document.getElementsByClassName('message bot-message');
	if (botMessages.length === 0) {
		return;
	}
	const loadingIndicator = botMessages[botMessages.length - 1];
	if (loadingIndicator.querySelector('svg')) {
		chatBox.removeChild(loadingIndicator);
	}
}

function startBotMessage() {
	removeLoading();
	streamingText = "";
	const chatBox = document.getElementById('chatBox');
	const message = document.createElement('div');
	message.className = 'message bot-message streaming';
	chatBox.appendChild(message);
	chatBox.scrollTop = chatBox.scrollHeight;
}

function appendBotChunk(chunk) {
	streamingText += chunk;
	const bubbles = document.getElementsByClassName('streaming');
	if (bubbles.length === 0) {
		return;
	}
	const bubble = bubbles[bubbles.length - 1];
	bubble.textContent = streamingText;
	const chatBox = document.getElementById('chatBox');
	chatBox.scrollTop = chatBox.scrollHeight;
}

function finishBotMessage(fullText) {
	const bubbles = document.getElementsByClassName('streaming');
	if (bubbles.length > 0) {
		const bubble = bubbles[bubbles.length - 1];
		bubble.classList.remove('streaming');
		bubble.innerHTML = marked.parse(fullText);
	}
	streamingText = "";
	unblockInput();
	updatePlaceholder();
}

function markCancelled() {
	removeLoading();
	const bubbles = document.getElementsByClassName('streaming');
	if (bubbles.length > 0) {
		const bubble = bubbles[bubbles.length - 1];
		bubble.classList.remove('streaming');
		bubble.textContent = (bubble.textContent || "") + " (cancelled)";
	} else {
		const chatBox = document.getElementById('chatBox');
		const message = document.createElement('div');
		message.className = 'message bot-message';
		message.textContent = "(cancelled)";
		chatBox.appendChild(message);
	}
	streamingText = "";
	unblockInput();
}

function showError(message) {
	removeLoading();
	addBotMessage(message);
	unblockInput();
	updatePlaceholder();
}

function showApproval(summary) {
}

function stopGeneration() {
	cancelAIResponse();
}

document.addEventListener('keydown', function(event) {
	if (event.key === 'Enter') {
		sendMessage();
	}
});