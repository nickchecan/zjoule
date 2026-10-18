# End-to-end UI tests

The `com.developer.nefarious.zjoule.e2e` module drives the Eclipse workbench with [SWTBot](https://eclipse.dev/swtbot/). The tests open the zJoule view, the login wizard, and the preference page. They do not call SAP AI Core or Ollama, and they do not need a service key.

ABAP Development Tools (ADT) are not required. The suite boots the Eclipse Platform product `org.eclipse.platform.ide` with `org.eclipse.ui.ide.workbench`. Tycho installs `com.developer.nefarious.zjoule.plugin` into that runtime because the test bundle requires it.

The module is not part of `./mvnw clean verify`. The root `e2e` profile adds it to the reactor. The release workflow (`.github/workflows/main.yaml`) is unchanged.

The view is found by the contribution id `com.developer.nefarious.zjoule.plugin.core.ui.ViewListener`. The HTML document title inside the chat browser is not asserted. The Show View dialog accepts a category child named `zjoule` or `zJoule`.

## What the suite checks

`ZJouleWorkbenchUiTest` covers four cases:

1. Open the view by id. The login toolbar action (tooltip `Click to log in to your BTP subaccount.`) is enabled, and the view menu contains Clear Chat, Preferences, and Logout.
2. Open the view from **Window > Show View > Other...** under the **ABAP Copilot** category.
3. Click the login action. The wizard title is `Setup AI Provider`. The **SAP AI Core** and **Ollama (Local)** radios are visible, and so is the **Select** button. The test clicks **Cancel**. It does not finish the wizard.
4. Click **Preferences** on the view menu. The **ABAP Copilot** page is present and shows the **Instructions:** field. The test clicks **Cancel**.

Screenshots are written to `com.developer.nefarious.zjoule.e2e/target/e2e-screenshots/` (`zjoule-view.png`, `login-wizard.png`, `preferences.png`). SWTBot also writes a failure screenshot when a test throws.

## Command line

From the repository root. The first run downloads the Eclipse platform and SWTBot into the local Maven repository.

### macOS

A GUI session is required. Cocoa SWT cannot create a display from a plain SSH session.

```sh
./mvnw -Pe2e clean verify -pl com.developer.nefarious.zjoule.e2e -am
```

On macOS the `mac-e2e` profile activates by itself and prepends `-XstartOnFirstThread` to the test JVM. Do not add that flag on Linux. You do not pass `-Pmac-e2e` yourself.

The SWT fragment is chosen from the machine running Maven:

| Machine | Fragment |
| --- | --- |
| Apple Silicon | `org.eclipse.swt.cocoa.macosx.aarch64` |
| Intel Mac | `org.eclipse.swt.cocoa.macosx.x86_64` |

Both environments are already listed in `maven/pom.xml`. The fragment comes from the Eclipse release repository (`https://download.eclipse.org/releases/latest/`). There is no extra macOS repository to add. The system WebKit framework backs the chat view's SWT `Browser`; Homebrew WebKit is not required.

`-XstartOnFirstThread` makes the process main thread the AppKit thread. `useUIThread` stays `false`, so SWTBot posts events from a background thread. That pair is the supported macOS setup.

### Linux

Install GTK 3, WebKitGTK 4.1 (the SWT browser used by current Eclipse), and a virtual framebuffer:

```sh
sudo apt-get update
sudo apt-get install -y libwebkit2gtk-4.1-0 libgtk-3-0 xvfb
```

Then:

```sh
export WEBKIT_DISABLE_COMPOSITING_MODE=1
export WEBKIT_DISABLE_DMABUF_RENDERER=1
xvfb-run -a --server-args="-screen 0 1600x1200x24" \
  ./mvnw -Pe2e clean verify -pl com.developer.nefarious.zjoule.e2e -am
```

The same command is what [`.github/workflows/e2e.yaml`](../.github/workflows/e2e.yaml) runs. That workflow is `workflow_dispatch` only. It does not run on push, and it does not participate in release gating.

### What `./mvnw clean verify` does

Without `-Pe2e`, the root reactor is the plug-in, the unit-test bundle, the feature, and the update site. `com.developer.nefarious.zjoule.e2e` is not built. To confirm:

```sh
./mvnw -q -DforceStdout help:evaluate -Dexpression=project.modules
./mvnw -Pe2e -q -DforceStdout help:evaluate -Dexpression=project.modules
```

The second listing includes `com.developer.nefarious.zjoule.e2e`.

`-pl com.developer.nefarious.zjoule.e2e -am` builds the plug-in and the UI tests. It does not rebuild the unit-test bundle or the update site.

## Running the tests inside Eclipse

Use the same Eclipse release you use for the plug-in (2024-09 or newer; this build resolves the current simultaneous release). Import the Maven projects so PDE can resolve the test bundle.

### 1. Install SWTBot into the IDE

The test bundle depends on SWTBot bundles that are not part of the Eclipse SDK. Install them into the IDE you will launch from:

1. **Help > Install New Software...**
2. **Add...** and set the location to `https://download.eclipse.org/technology/swtbot/releases/latest/`
3. If that site's SWTBot is older than 4.4 and the test bundle stays unresolved, use `https://download.eclipse.org/technology/swtbot/snapshots/` instead. The Maven build uses the snapshots site because SWTBot 4.4 matches the Eclipse 2026-09 train (Hamcrest 3). The IDE only needs the bundles so PDE can compile and launch.
4. Select these features:
   - **SWTBot for Eclipse Testing**
   - **SWTBot IDE Integration** (this adds **Run As > SWTBot Test**)
   - **SWTBot - JUnit5 Support**
5. Finish the wizard and restart Eclipse.

If `com.developer.nefarious.zjoule.e2e` still reports missing SWTBot constraints, open **Window > Preferences > Plug-in Development > Target Platform**, select the running platform, and **Apply**.

### 2. Import the module

The e2e module is outside the default reactor, so a plain import of the root pom does not show it until the profile is on.

1. **File > Import > Maven > Existing Maven Projects**.
2. Choose the repository root and import the projects Maven offers.
3. Right-click the root `zjoule` project, **Maven > Select Maven Profiles...**, enable `e2e`, then **Maven > Update Project**.
4. If the e2e project still does not appear, import `com.developer.nefarious.zjoule.e2e` on its own: **File > Import > Maven > Existing Maven Projects** and select that directory. Its parent is `maven/pom.xml`, resolved by relative path.

You can also use **File > Import > General > Existing Projects into Workspace** and select `com.developer.nefarious.zjoule.e2e`. The `.project` has the PDE and Maven natures.

### 3. Launch

**Run As > SWTBot Test** (preferred):

1. In Package Explorer, right-click `ZJouleWorkbenchUiTest`.
2. **Run As > SWTBot Test**.
3. If Eclipse opens the launch configuration instead of running:
   - **Test** tab: **Test runner** = **JUnit 5**.
   - **Main** tab (or the tab that shows it): clear **Run in UI thread**. SWTBot must run off the UI thread. The SWTBot launch shortcut does this; a reused JUnit Plug-in Test launch often does not.
   - **Arguments** tab, **VM arguments**: on macOS, include `-XstartOnFirstThread`. Leave it out on Linux and Windows. A useful line on macOS is `-XstartOnFirstThread -Xms256m -Xmx2048m -Dorg.eclipse.swtbot.search.timeout=20000`.
   - **Working directory**: set it to the e2e project, `${workspace_loc:com.developer.nefarious.zjoule.e2e}`, so screenshots land in that project's `target/e2e-screenshots`.
   - **Plug-ins** tab: **Launch with** = **all workspace and enabled target plug-ins**. `com.developer.nefarious.zjoule.plugin` and the SWTBot bundles must be selected. Deselect other copies of the plug-in if both a workspace project and a target copy are checked.
   - Clear the workspace (**Main** tab, **Clear** workspace data) the first time so a previous session does not open extra dialogs.
4. Run.

**Run As > JUnit Plug-in Test** if the SWTBot shortcut is not installed:

1. Right-click `ZJouleWorkbenchUiTest` > **Run As > JUnit Plug-in Test**.
2. Set the same options as above. Uncheck **Run in UI thread** before the first run. On macOS add `-XstartOnFirstThread` to the VM arguments.
3. **Test runner** must be **JUnit 5**, not JUnit 4.

### 4. Where the results are

- The **JUnit** view lists each method. Open it with **Window > Show View > Other... > Java > JUnit** if it did not open.
- The **Console** shows the workbench log (`-consoleLog` is set for the Maven run; the IDE console shows the same stream).
- Passing Maven runs leave PNGs in `com.developer.nefarious.zjoule.e2e/target/e2e-screenshots/`.
- A failed SWTBot test also writes a screenshot. From the IDE that file is under the launch working directory, in `screenshots/`, unless the test reached `capture` and wrote `target/e2e-screenshots/` first.

## Adding a test

1. Add a method annotated with `@Test` to `ZJouleWorkbenchUiTest`, or add a new class in package `com.developer.nefarious.zjoule.e2e` whose name ends with `Test`. Tycho Surefire picks up `**/*Test.java`. Put `@ExtendWith(SWTBotJunit5Extension.class)` on a new class so a failure captures the display.
2. Open the view with `bot.viewById("com.developer.nefarious.zjoule.plugin.core.ui.ViewListener")`. Do not assert the HTML title inside the browser.
3. Keep the test local. Do not finish the login wizard, and do not point it at a real endpoint. Cancel dialogs. `@AfterEach` already closes `Setup AI Provider`, `Preferences`, and `Show View` when they are still up.
4. Reuse the SWTBot timeout already set by Maven (`org.eclipse.swtbot.search.timeout=20000`). Avoid fixed sleeps.
5. Run `./mvnw -Pe2e verify -pl com.developer.nefarious.zjoule.e2e -am` before pushing.

The class has to follow the same Checkstyle rules as the rest of the repo (`maven/checkstyle.xml`): Javadoc on the type and on public methods, final parameters, and the import groups `java`, `javax`, `org`, `com`.

## Layout

```
com.developer.nefarious.zjoule.e2e/
  META-INF/MANIFEST.MF          SWTBot 4.4 and JUnit Jupiter requirements
  plugin_customization.ini      no intro, Resource perspective
  pom.xml                       eclipse-test-plugin, UI harness, macOS VM flag
  src/.../ZJouleWorkbenchUiTest.java
```

`pom.xml` sets `useUIHarness=true`, `useUIThread=false`, product `org.eclipse.platform.ide`, and application `org.eclipse.ui.ide.workbench`. The parent Surefire configuration leaves `useUIHarness` false; this module overrides it. Surefire is pinned to the `junit6` provider. SWTBot also requires JUnit 4, and Tycho would otherwise select the JUnit 4 provider and report that no tests were found. The Eclipse 2026-09 repository resolves `junit-jupiter-api` 6. The chat view creates an SWT `Browser` (WebKit on macOS and Linux). That is why Linux needs `libwebkit2gtk-4.1-0` and a display. The Surefire configuration sets `WEBKIT_DISABLE_COMPOSITING_MODE` and `WEBKIT_DISABLE_DMABUF_RENDERER` so WebKit can paint under Xvfb.
