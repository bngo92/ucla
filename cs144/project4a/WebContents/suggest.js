function AutoSuggestControl(oTextbox, oProvider) {
    this.provider = oProvider;
    this.textbox = oTextbox;
    this.init();
}

AutoSuggestControl.prototype.selectRange = function (iStart, iLength) {
    var oRange = this.textbox.createTextRange();
    oRange.moveStart("character", iStart);
    oRange.moveEnd("character", iLength - this.textbox.value.length);
    oRange.select();
};

AutoSuggestControl.prototype.autosuggest = function (aSuggestions) {
    if (aSuggestions.length > 0) {
        this.typeAhead(aSuggestions[0]);
    }
};

AutoSuggestControl.prototype.handleKeyUp = function (oEvent) {
    var iKeyCode = oEvent.keyCode;
    if (iKeyCode < 32 || (iKeyCode >= 33 && iKeyCode <= 46) || (iKeyCode >= 112 && iKeyCode <= 123)) {
        // ignore
    } else {
        this.provider.requestSuggestions(this);
    }
};

AutoSuggestControl.prototype.init = function () {
    var oThis = this;
    this.textbox.onkeyup = function (oEvent) {
        if (!oEvent) {
            oEvent = window.event;
        }
        oThis.handleKeyUp(oEvent);
    }
};

function SuggestionProvider() {

}

SuggestionProvider.prototype.requestSuggestions = function (oAutoSuggestControl) {
    var aSuggestions = [];
    var sTextBoxValue = oAutoSuggestControl.textbox.value;
    if (sTextBoxValue.length > 0) {
        for (var i = 0; i < this.suggestions.length; i++) {
            if (this.suggestions].indexOf(sTextboxValue) == 0) {
                aSuggestions.push(this.suggestions]);
            }
        }
        oAutoSuggestControl.autosuggest(aSuggestions);
    }
};

function Suggestions() {
    this.suggestions = [
    ];
}
