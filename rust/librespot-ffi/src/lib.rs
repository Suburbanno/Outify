use librespot_api::Engine;

#[uniffi::export]
pub fn login(user: String, pass: String) -> Result<(), SpotifyError> {
    Engine::global().login(user,pass)?;
    Ok(())
}
