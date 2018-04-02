document.querySelector("[name='timezone']").value =
  Intl.DateTimeFormat().resolvedOptions().timeZone;

const dateTimePickerFormat = 'YYYY-MM-DD HH:mm';

$(".date").datetimepicker({format: dateTimePickerFormat});

function enableTimeOffsetButtons(targetName) {
  const input = document.querySelector("[name='" + targetName + "']");
  const buttons = document.querySelectorAll(
    "[data-button-target-name='" + targetName + "'] button[data-time-offset]");
  for (let b = 0; b < buttons.length; ++b) {
    buttons[b].addEventListener("click", function(event) {
      const dataTimeOffset = event.target.getAttribute("data-time-offset");
      if (dataTimeOffset == "") {
        input.value = "";
      } else {
        const dataTimeOffsetAmount =
            event.target.getAttribute("data-time-offset-amount");
        input.value = moment().subtract(dataTimeOffsetAmount, dataTimeOffset)
            .format(dateTimePickerFormat);
      }
    });
  }
}

enableTimeOffsetButtons("from");
enableTimeOffsetButtons("to");
