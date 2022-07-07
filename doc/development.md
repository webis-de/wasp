WASP Development
================

```
sudo docker build -t ghcr.io/webis-de/wasp:dev .
sudo docker run -p 127.0.0.1:8001:8001 -p 127.0.0.1:8002:8002 --name wasp ghcr.io/webis-de/wasp:dev
sudo docker cp wasp:/home/user/app/pywb/proxy-certs/pywb-ca.pem .
```

WARNING: Failed to index record <urn:uuid:b0efea66-fe30-11ec-af6f-0242ac110002> of type response
java.lang.NullPointerException: Cannot invoke "java.lang.CharSequence.length()" because "<parameter1>" is null
	at net.htmlparser.jericho.Source.<init>(Source.java:117)
	at de.webis.wasp.warcs.JerichoDocumentExtractor.apply(JerichoDocumentExtractor.java:31)
	at de.webis.wasp.warcs.JerichoDocumentExtractor.apply(JerichoDocumentExtractor.java:18)
	at de.webis.wasp.warcs.GenericHtmlWarcRecordConsumer.acceptHtmlResponse(GenericHtmlWarcRecordConsumer.java:76)
	at de.webis.wasp.warcs.GenericWarcRecordConsumer.acceptResponse(GenericWarcRecordConsumer.java:67)
	at de.webis.wasp.warcs.GenericWarcRecordConsumer.accept(GenericWarcRecordConsumer.java:42)
	at de.webis.wasp.warcs.GenericWarcRecordConsumer.accept(GenericWarcRecordConsumer.java:19)
	at de.webis.wasp.warcs.ContinuousWarcRecordReader.consume(ContinuousWarcRecordReader.java:91)
	at de.webis.wasp.warcs.WarcRecordReader.run(WarcRecordReader.java:134)
