package com.ds.coursework2.naming;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

public class NameServerClient {
    private EtcdClient etcdClient;

    public NameServerClient(String nameServiceAddress) throws IOException {
        etcdClient = new EtcdClient(nameServiceAddress);
    }

    public static String buildServerDetailsEntry(String serviceAddress, int port, String protocol) {
        return new JSONObject()
                .put("ip", serviceAddress)
                .put("port", Integer.toString(port))
                .put("protocol", protocol)
                .toString();
    }

    public static String buildServerDetailsEntry(String[] serviceNames) {
        JSONObject json = new JSONObject();
        for (int i = 0; i < serviceNames.length; i++) {
            String service = serviceNames[i];
            json.put("service " + (i + 1), service);
        }
        return json.toString();
    }

    public ServiceDetails findService(String serviceName)
            throws InterruptedException, IOException {
        System.out.println("Searching for details of service :" + serviceName);
        String etcdResponse = etcdClient.get(serviceName);
        ServiceDetails serviceDetails = new ServiceDetails().populate(etcdResponse);
        while (serviceDetails == null) {
            System.out.println("Couldn't find details of service" + serviceName + ", retrying in 5 seconds.");
            Thread.sleep(5000);
            etcdResponse = etcdClient.get(serviceName);
            serviceDetails = new ServiceDetails().populate(etcdResponse);
        }
        return serviceDetails;
    }

    public ServiceDetails findServer(String serverName)
            throws InterruptedException, IOException {
        System.out.println("Searching for details of service :" + serverName);
        String etcdResponse = etcdClient.get(serverName);
        ServiceDetails serviceDetails = new ServiceDetails().populateServerInfo(etcdResponse);
        while (serviceDetails == null) {
            System.out.println("Couldn't find details of service" + serverName + ", retrying in 5 seconds.");
            Thread.sleep(5000);
            etcdResponse = etcdClient.get(serverName);
            serviceDetails = new ServiceDetails().populateServerInfo(etcdResponse);
        }
        return serviceDetails;
    }

    public void registerService(String serviceName, String IPAddress, int port, String protocol)
            throws IOException {
        String serviceInfoValue = buildServerDetailsEntry(IPAddress, port, protocol);
        etcdClient.put(serviceName, serviceInfoValue);
    }

    public void registerServer(String IPAddress, String[] services) throws IOException {
        String servicesInfo = buildServerDetailsEntry(services);
        etcdClient.put(IPAddress, servicesInfo);
    }

    public class ServiceDetails {
        private String IPAddress;
        private int port;
        private String protocol;
        private String service1, service2, service3;

        ServiceDetails populate(String serverResponse) {
            JSONObject serverResponseJSONObject = new JSONObject(serverResponse);
            if (serverResponseJSONObject.has("kvs")) {
                JSONArray values = serverResponseJSONObject.getJSONArray("kvs");
                JSONObject firstValue = (JSONObject) values.get(0);
                String encodedValue = (String) firstValue.get("value");
                byte[] serverDetailsBytes = Base64.getDecoder().decode(encodedValue.getBytes(StandardCharsets.UTF_8));
                JSONObject serverDetailsJson = new JSONObject(new String(serverDetailsBytes));
                IPAddress = serverDetailsJson.get("ip").toString();
                port = Integer.parseInt(serverDetailsJson.get("port").toString());
                protocol = serverDetailsJson.get("protocol").toString();
                return this;
            } else {
                return null;
            }
        }

        ServiceDetails populateServerInfo(String serverResponse) {
            JSONObject serverResponseJSONObject = new JSONObject(serverResponse);
            if (serverResponseJSONObject.has("kvs")) {
                JSONArray values = serverResponseJSONObject.getJSONArray("kvs");
                JSONObject firstValue = (JSONObject) values.get(0);
                String encodedValue = (String) firstValue.get("value");
                byte[] serverDetailsBytes = Base64.getDecoder().decode(encodedValue.getBytes(StandardCharsets.UTF_8));
                JSONObject serverDetailsJson = new JSONObject(new String(serverDetailsBytes));
                service1 = serverDetailsJson.get("service 1").toString();
                service2 = serverDetailsJson.get("service 2").toString();
                service3 = serverDetailsJson.get("service 3").toString();
                return this;
            } else {
                return null;
            }
        }

        public String getIPAddress() {
            return IPAddress;
        }

        public int getPort() {
            return port;
        }

        public String getProtocol() {
            return protocol;
        }

        public String getService1() {
            return service1;
        }

        public String getService2() {
            return service2;
        }

        public String getService3() {
            return service3;
        }
    }
}
