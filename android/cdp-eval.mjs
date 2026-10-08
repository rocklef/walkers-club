// Debug helper: node cdp-eval.mjs "<js expression>" — evaluates in the app's WebView over adb-forwarded port 9333.
const list = await (await fetch("http://127.0.0.1:9333/json")).json();
const ws = new WebSocket(list[0].webSocketDebuggerUrl);
const expr = process.argv[2];
ws.onopen = () => ws.send(JSON.stringify({id: 1, method: "Runtime.evaluate", params: {expression: expr, returnByValue: true, awaitPromise: true, userGesture: true}}));
ws.onmessage = (m) => {
  const msg = JSON.parse(m.data);
  if (msg.id !== 1) return;
  const r = msg.result?.result;
  console.log(JSON.stringify(r?.value ?? r ?? msg, null, 1));
  ws.close();
};
