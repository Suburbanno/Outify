// Calls the UI callback for song change.
// Updates the UI
pub fn on_song_changed() {
    let jvm = match crate::JVM.get() {
        Some(j) => j,
        None => {
            error!("failed to propagate song change to the ui: jvm not initialized");
            return;
        }
    };

    let _env = match jvm.get_env() {
        Ok(e) => e,
        Err(e) => {
            error!("failed to propagate song change to the ui: {}", e);
            return;
        }
    };
}
