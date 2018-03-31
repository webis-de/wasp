document.querySelector("[name='timezone']").value =
  Intl.DateTimeFormat().resolvedOptions().timeZone;

$(".date").datetimepicker(
  {format: 'YYYY-MM-DD HH:mm'}
);
