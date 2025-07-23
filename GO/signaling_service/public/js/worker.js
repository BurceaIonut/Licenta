`use strict`;

function createSenderTransform() {
return new TransformStream({
    start() {

    },

    async transform(encodedFrame, controller) {
        console.log("Frame sent");
    let view = new DataView(encodedFrame.data);

    let newData = new ArrayBuffer(encodedFrame.data.byteLength + 4);
    let newView = new DataView(newData);

    for (let i = 0; i < encodedFrame.data.byteLength; ++i)
        newView.setInt8(i, ~view.getInt8(i));

    for (let i = 0; i < 4; ++i)
        newView.setInt8(encodedFrame.data.byteLength + i, 0);

    encodedFrame.data = newData;

    controller.enqueue(encodedFrame);
    },

    flush() {

    }
});
}

function createReceiverTransform() {
return new TransformStream({
    start() {},
    flush() {},
    async transform(encodedFrame, controller) {
        console.log("Frame received");

    const view = new DataView(encodedFrame.data);

    const newData = new ArrayBuffer(encodedFrame.data.byteLength - 4);
    const newView = new DataView(newData);

    for (let i = 0; i < encodedFrame.data.byteLength - 4; ++i)
        newView.setInt8(i, ~view.getInt8(i));

    encodedFrame.data = newData;
    controller.enqueue(encodedFrame);
    }
});
}

function createGenericTransform() {
    return new TransformStream({
        start() {},
        flush() {},
        async transform(encodedFrame, controller) {
            console.log("Frame received");
            const view = new DataView(encodedFrame.data);

            const newData = new ArrayBuffer(encodedFrame.data.byteLength);
            const newView = new DataView(newData);

            for (let i = 0; i < encodedFrame.data.byteLength; ++i)
                newView.setInt8(i, ~view.getInt8(i));

            encodedFrame.data = newData;
            controller.enqueue(encodedFrame);
        }
    })
}

onrtctransform = (event) => {
    let transform;
    if (event.transformer.options.name == "senderTransform")
        transform = createSenderTransform();
    else if (event.transformer.options.name == "receiverTransform")
        transform = createReceiverTransform();
    else
        transform = createGenericTransform();
    event.transformer.readable
        .pipeThrough(transform)
        .pipeTo(event.transformer.writable);
    };