module UrlHelpers
  def absolute_url(path)
    URI::Generic.build(
      scheme: 'https',
      host: host,
      path: path
    ).to_s
  end
end
