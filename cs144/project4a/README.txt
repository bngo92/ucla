Rachel Fang 104001868
Bryan Ngo 503901486

Question 1:
We use SSL for (2)-(3)-(4)-(5)-(6), which are all the interactions with the pay servlet

Question 2:
Buy price is stored on the server, so it is immutable from the client-end.

Question 3:
using meta viewport should autowrap the text, but just in case, we have a media query hiding all horizontal overflow if the device is mobile sized.

Question 4:
Setting the width in meta viewport to device-width allows the text to be wrapped to the size of the mobile device. In addition, we use a media query (the same one) to increase the font size to make text easier to read on mobile screens.
