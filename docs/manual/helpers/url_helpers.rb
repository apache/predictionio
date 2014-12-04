module UrlHelpers
  def absolute_url(path)
    URI::Generic.build(
      scheme: scheme,
      host: host,
      port: port.to_i,
      path: path
    ).to_s
  end
end