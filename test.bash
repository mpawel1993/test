oc get pods -n splunk-monitoring
oc logs <sck-pod> | grep -i "scrape\|error\|jvm\|9090"