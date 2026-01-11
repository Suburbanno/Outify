use librespot_api::oauth_get_access_token;
use librespot_core::Error;

#[tokio::main]
async fn main() -> Result<(), Error> {
    //let token = oauth_get_access_token().await?;
    //println!("Access token: {}", token.access_token);
    //println!("Refresh token: {}", token.refresh_token);

    test_auth_url().await?;
    Ok(())
}

async fn test_auth_url() -> Result<(), Error> {
    let url = librespot_api::oauth_get_auth_url().await?;
    println!("{}", url);
    Ok(())
}
