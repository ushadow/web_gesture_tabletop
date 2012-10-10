class HandInputView
  constructor: ->
    @onConnect = ->
    @onDisconnect = ->

    @$status = $('#status')
    @$button = $('button')
    @$button.click => @onButtonClick()
    @canvas = document.getElementById 'canvas'
    @canvasHeight = @canvas.clientHeight
    @canvasWidth = @canvas.clientWidth

  showInfo: (message) ->
    @$status.text message
    switch message
      when 'Connected'
        @$button.html 'Disconnect'
      when 'Disconnected'
        @$button.html 'Connect'

  # Draws a rectangle with top left corner at (x, y).
  drawRect: (x, y) ->
    context = @canvas.getContext '2d'
    context.clearRect 0, 0, @canvasWidth, @canvasHeight
    context.fillStyle = '#ff0000'
    [canvasX, canvasY] = screenToCanvasCoord x, y
    context.fillRect canvasX, canvasY, 10, 10

  onButtonClick: ->
    if @$button.html() is 'Connect'
      @onConnect()
    else
      @onDisconnect()

  # Converts screen coordinate to canvas coordinate.
  #
  # @param {int} x screen coordinate where top left is the origin.
  # @param {int} y screen coordinate where top left is the origin.
  screenToCanvasCoord: (x, y) ->
    canvasX = x - windows.screenX - (window.outerWidth - window.innerWidth) -
      @canvas.offsetLeft
    canvasY = y - windows.screenY - (window.outerHeight - window.innerHeight) -
      @canvas.offsetTop
    [canvasX, canvasY]
