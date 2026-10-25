# UI tests

The `e2e` profile launches a real Eclipse workbench with SWTBot. The suite does not call SAP AI Core or Ollama. Surefire sets `zjoule.e2e.scripted=true`, and the chat view uses a fixed reply.

```bash
xvfb-run -a --server-args="-screen 0 1600x1200x24" \
  ./mvnw -Pe2e clean verify \
  -pl com.developer.nefarious.zjoule.libs,com.developer.nefarious.zjoule.plugin,com.developer.nefarious.zjoule.e2e -am
```

The libs module is listed because the plug-in requires that bundle and does not declare a Maven dependency on it. The default `./mvnw clean verify` does not run this profile.
